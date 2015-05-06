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
  def macroTransform(annottees: Any*): Any = macro SyncSchemaMacros.process
}

trait Empty

class SyncSchemaMacros(ct: Context)
extends SchemaMacros(ct)
{
  import c.universe._

  case class UuidColSpec()
  extends AttrSpecBase
  {
    def name = TermName("uuid")

    def tpt = tq"Option[String]"

    override def default = q"None"
  }

  class SyncModelOps(m: ModelSpec)
  extends ModelOps(m)
  {
    lazy val uuidColumn = UuidColSpec()

    lazy val mapperFields = uuidColumn :: attrs ++ foreignKeys ++ assocs

    lazy val mapperFieldStrings = mapperFields.map(_.name.toString)

    override def extraColumns = List(uuidColumn)

    override def modelBases = super.modelBases :+ tq"Sync"

    override def tableBases = List(tq"SyncTable[$name]")

    override def queryBase = tq"Empty"

    override def queryType =
      tq"SyncTableQuery[$name, $tableName, $mapperType]"

    override def queryExtra = {
      List(
        q"""
        implicit def encodeJson($session) = ${TermName(s"${name}EncodeJson")}
        """,
        q"""
        implicit def mapperCodecJson($session) =
          ${TermName(s"${name}MapperCodecJson")}
        """
      ) ++ handleMapper
    }

    def handleMapper = {
      val mapper = mapperType
      val att = attrs map { f ⇒ q"mapper.${f.term}" }
      val fks = foreignKeys map { f ⇒
        q"""
        ${f.query}.idByUuid(mapper.${f.term}).getOrElse(uuidError(mapper))
        """
      }
      val fields = att ++ fks :+ q"uuid = Some(mapper.uuid)"
      val assocUpdates = assocs map { f ⇒
        q"""
        obj.${f.replaceMany}(${f.query}.idsByUuids(mapper.${f.term}))
        """
      }
      List(
        q"""
        def syncFromMapper(mapper: $mapperType)($session) {
          idByUuid(mapper.uuid) some {
            id ⇒ updateFromMapper(id, mapper) } none {
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
        def applyMapper(id: Option[Long], mapper: $mapper, app: $name ⇒ Any)
        ($session) {
          val obj = $term(id, ..$fields)
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

    def encodeJson = {
      val ident = TermName(s"${name.toString}EncodeJson")
      val fks = foreignKeys map { a ⇒ (a.nameS, q"obj.${a.load}.uuid") }
      val ass = assocs map { a ⇒
        (a.nameS, q"obj.${a.loadMany}.list.uuids")
      }
      val att = attrs map { a ⇒ (a.nameS, q"obj.${a.term}") }
      val (names, values) = (fks ++ ass ++ att).unzip
      val encoder = TermName(s"jencode${values.length}L")
      q"""
      implicit def $ident ($session) =
        $encoder { obj: $name ⇒ (..$values) }(..$names)
      """
    }

    def mapperCodec = {
      val ident = TermName(s"${name.toString}MapperCodecJson")
      val names = mapperFieldStrings
      val decoder = TermName(s"casecodec${names.length}")
      q"""
      implicit def $ident ($session) =
        $decoder($mapperTerm.apply, $mapperTerm.unapply)(..$names)
      """
    }

    def jsonCodec = List(encodeJson, mapperCodec)

    def backendMapper = {
      q"""
      case class $mapperType(..$mapperParams)
      extends BackendMapper[$name]
      """
    }

    lazy val mapper = s"${name}Mapper"

    lazy val mapperType = TypeName(mapper)

    lazy val mapperTerm = TermName(mapper)

    lazy val mapperParams = {
      val atts = attrs map { _.valDef }
      val fks = foreignKeys map { a ⇒ q"val ${a.term}: String" }
      val ass = assocs map { a ⇒ q"val ${a.term}: Seq[String]" }
      val uuid = q"val uuid: String"
      uuid :: atts ++ fks ++ ass
    }
  }

  class SyncAssocOps(a: AssocSpec)
  extends AssocOps(a)

  override implicit def modelOps(cls: ModelSpec) = new SyncModelOps(cls)
  override implicit def assocOps(cls: AssocSpec) = new SyncAssocOps(cls)

  implicit object SyncEnumProcessor
  extends BasicEnumProcessor[SyncSchemaMacros]
  {
    override def apply(enum: EnumSpec) = super.apply(enum) :+ jsonEncode(enum)

    def jsonEncode(enum: EnumSpec) = {
      val name = enum.name
      val nameS = name.toString
      val tpe = TypeName(nameS)
      val ident = TermName(s"enum${name}Codec")
      q"""
      implicit val $ident = jencode1L { v: $name.$tpe ⇒ v.toString } ($nameS)
      """
    }
  }

  class SyncTransformer(cls: TableSpec)
  extends Transformer(cls)
  {

  }

  object SyncTransformer
  {
    def apply(cls: TableSpec) = new SyncTransformer(cls)
  }

  override def transform(cls: TableSpec) = SyncTransformer(cls).result

  override def dateTime = {
    super.dateTime ++ Seq(
      q"""
      implicit val dateTimeJsonFormat =
        jencode1L { (dt: DateTime) ⇒ dt.getEra } ("time")
      """
    )
  }

  override def extraPre(classes: List[ModelSpec]): List[Tree] = {
    List(
      q"import argonaut._",
      q"import Argonaut._"
    )
  }

  override def extra(classes: List[ModelSpec]) = {
    val jsonCodecs = classes.map(_.jsonCodec).flatten
    val backendMappers = classes.map(_.backendMapper)
    List(
      q"import PendingActionsSchema._",
      q"val pendingActions = PendingActionsSchema.PendingActionSet",
      q"val pendingMetadata = PendingActionsSchema.metadata",
      q"val syncMetadata = ${syncMetadata(classes)}"
    ) ++ backendMappers ++ jsonCodecs
  }

  def syncMetadata(classes: List[ModelSpec]) = {
    val data = classes map { cls ⇒
      val meta = q"""
      db.SyncTableMetadata[${cls.name}, ${cls.tableName},
      ${cls.mapperType}](
        ${cls.path}, ${cls.query}
      )
      """
      (cls.path, meta)
    }
    q"db.SyncSchemaMetadata(Map(..$data))"
  }

  override val extraBases =
    List(tq"slick.SyncSchemaBase")
}
