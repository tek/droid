package slick

import scala.slick.driver.SQLiteDriver.simple._
import scala.reflect.macros.whitebox.Context
import scala.annotation.StaticAnnotation

trait SyncSchemaBase
extends SchemaBase
{
  def syncMetadata: db.SyncSchemaMetadata
}

class SyncSchema
extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro SyncSchemaMacros.create
}

trait Empty

class SyncSchemaMacros(ct: Context)
extends SchemaMacros(ct)
{
  import c.universe._

  def jsonEnumCodec(tree: Tree) = {
    val q"object $name extends ..$bases { ..$body }" = tree
    val nameS = name.toString
    val tpe = TypeName(nameS)
    val ident = TermName(s"enum${name}Codec")
    q"""
    implicit val $ident = jencode1L { v: $name.$tpe ⇒ v.toString } ($nameS)
    """
  }

  override def enum(tree: Tree) = super.enum(tree) :+ jsonEnumCodec(tree)

  override def model(cls: ClsDesc, augment: Boolean = true) = {
    val base = super.model(cls, augment)
    if (!augment) base
    else {
      val q"case class $name(..$fields) extends ..$bases { ..$body }" = base
      val uuidval = q"val uuid: Option[String] = None"
      q"""
      case class $name (..$fields, $uuidval)
      extends ..$bases
      with db.Sync
      {
        ..$body
      }
      """
    }
  }

  // TODO merge with query?
  def modelCompanion(cls: ClsDesc) = {
    val comp = cls.compName
    val fromJson = {
      val name = cls.typeName
      val attrs = cls.attrs map { a ⇒
        q"val ${a.term}: ${TypeName(a.typeName)}"
      }
      val fks = cls.foreignKeys map { a ⇒ q"val ${a.term}: String" }
      val assocs = cls.assocs map { a ⇒ q"val ${a.term}: Seq[String]" }
      val fields = attrs ++ fks ++ assocs
      val attrArgs = cls.attrs map { _.term }
    }
    q"""
    object $comp
    {
    }
    """
  }

  override def tableBase(cls: ClsDesc) = tq"SyncTable[${cls.typeName}]"

  override def table(cls: ClsDesc, augment: Boolean = true)
  (implicit classes: List[ClsDesc]) =
  {
    val base = super.table(cls, augment)
    val q"class $name(..$fields) extends ..$bases { ..$body }" = base
    val uuidCol = q""" def uuid = column[Option[String]]("uuid") """
    q"""
    class $name (..$fields)
    extends ..$bases
    {
      ..$body
      $uuidCol
    }
    """
  }

  override def queryBase(cls: ClsDesc) =
    cls.assoc ? super.queryBase(cls) / tq"Empty"

  override def queryType(cls: ClsDesc) =
    cls.assoc ? super.queryType(cls) /
      tq"SyncTableQuery[${cls.typeName}, ${cls.tableType}, ${cls.mapperType}]"

  def handleMapper(cls: ClsDesc) = {
    val tp = cls.typeName
    val comp = cls.compName
    val mapper = cls.mapperType
    val attrs = cls.attrs map { f ⇒ q"mapper.${f.term}" }
    val fks = cls.foreignKeys map { f ⇒
      q"""
      ${f.query}.idByUuid(mapper.${f.term}).getOrElse(uuidError(mapper))
      """
    }
    val fields = attrs ++ fks :+ q"uuid = Some(mapper.uuid)"
    val assocUpdates = cls.assocs map { f ⇒
      q"""
      obj.${f.replaceMany}(${f.query}.idsByUuids(mapper.${f.term}))
      """
    }
    List(
    q"""
    def syncFromMapper(mapper: $mapper)($session) {
      idByUuid(mapper.uuid) some { id ⇒ updateFromMapper(id, mapper) } none {
        createFromMapper(mapper)
      }
    }
    """,
    q"""
    def updateFromMapper(id: Long, mapper: $mapper)($session) {
      applyMapper(Some(id), mapper, update)
    }
    """,
    q"""
    def createFromMapper(mapper: $mapper)($session) {
      applyMapper(None, mapper, insert)
    }
    """,
    q"""
    def applyMapper(id: Option[Long], mapper: $mapper, app: $tp ⇒ Any)
    ($session) {
      val obj = $comp(id, ..$fields)
      app(obj)
      ..$assocUpdates
    }
    """,
    q"""
    def uuidError(mapper: $mapper) = {
      throw new Exception(s"Invalid uuid found in mapper $$mapper")
    }
    """
  )
  }

  override def queryExtra(cls: ClsDesc): List[Tree] = {
    if (cls.assoc) super.queryExtra(cls)
    else List(
      q"""
      implicit def encodeJson($session) = ${TermName(s"${cls.name}EncodeJson")}
      """,
      q"""
      implicit def mapperCodecJson($session) =
        ${TermName(s"${cls.name}MapperCodecJson")}
      """,
      q"""
      val path = ${cls.plural}
      """
    ) ++ handleMapper(cls)
  }

  def encodeJson(cls: ClsDesc) = {
    val name = cls.typeName
    val comp = cls.compName
    val ident = TermName(s"${cls.name}EncodeJson")
    val encoder = TermName(s"jencode${cls.fields.length}L")
    val fks = cls.foreignKeys map { a ⇒ (a.name, q"obj.${a.load}.uuid") }
    val assocs = cls.assocs map { a ⇒
      (a.name, q"obj.${a.loadMany}.list.uuids")
    }
    val attrs = cls.attrs map { a ⇒ (a.name, q"obj.${a.term}") }
    val (names, values) = (fks ++ assocs ++ attrs).unzip
    q"""
    implicit def $ident ($session) =
      $encoder { obj: $name ⇒ (..$values) }(..$names)
    """
  }

  def mapperCodec(cls: ClsDesc) = {
    val comp = cls.compName
    val ident = TermName(s"${cls.name}MapperCodecJson")
    val names = cls.nonDateFieldNames
    val decoder = TermName(s"casecodec${names.length}")
    q"""
    implicit def $ident ($session) =
      $decoder(${cls.mapperTerm}.apply, ${cls.mapperTerm}.unapply)(..$names)
    """
  }

  def jsonCodec(cls: ClsDesc) = {
    List(encodeJson(cls), mapperCodec(cls))
  }

  def backendMapper(cls: ClsDesc) = {
    val name = cls.mapperType
    val fields = cls.mapperFields
    q"""
    case class $name(..$fields)
    extends BackendMapper[${cls.typeName}]
    """
  }

  override def dateTime = {
    super.dateTime ++ Seq(
      q"""
      implicit val dateTimeJsonFormat =
        jencode1L { (dt: DateTime) ⇒ dt.getEra } ("time")
      """
    )
  }

  override def extraPre(implicit classes: List[ClsDesc]): List[Tree] = {
    List(
      q"import argonaut._",
      q"import Argonaut._"
    )
  }

  override def extra(implicit classes: List[ClsDesc]) = {
    val modelComps = classes.map(modelCompanion)
    val jsonCodecs = classes.map(jsonCodec).flatten
    val backendMappers = classes.map(backendMapper)
    List(
      q"import PendingActionsSchema._",
      q"val pendingActions = PendingActionsSchema.pendingActionSets",
      q"val pendingMetadata = PendingActionsSchema.metadata",
      q"val syncMetadata = ${syncMetadata}"
    ) ++ modelComps ++ backendMappers ++ jsonCodecs
  }

  def syncMetadata(implicit classes: List[ClsDesc]) = {
    val data = classes map { cls ⇒
      val name = cls.query
      val meta = q"""
      db.SyncTableMetadata[${cls.typeName}, ${cls.tableType},
      ${cls.mapperType}](
        ${name.toString}, $name
      )
      """
      (name.toString, meta)
    }
    q"db.SyncSchemaMetadata(Map(..$data))"
  }

  override val extraBases =
    List(tq"slick.SyncSchemaBase")

  override def createClsDesc(tree: Tree, timestamps: Boolean) = {
    ClsDesc(tree, timestamps, true)
  }
}
