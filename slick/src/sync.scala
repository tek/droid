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
  }

  class SyncModelOps(m: ModelSpec)
  extends ModelOps(m)
  {
    lazy val uuidColumn = UuidColSpec()

    lazy val mapperFields = uuidColumn :: attrs ++ foreignKeys ++ assocs

    lazy val mapperFieldStrings = mapperFields.map(_.name.toString)

    def attrsWithUuid = attrs :+ uuidColumn

    override def extraColumns = uuidColumn :: super.extraColumns

    override def modelBases = super.modelBases :+ tq"slick.db.Sync"

    override def tableBases = List(tq"slick.db.SyncTable[$name]")

    override def queryBase = tq"slick.Empty"

    override def queryType =
      tq"slick.db.SyncTableQuery[$name, $tableName, $mapperType]"

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
      val mt = mapperType
      val att = attrsWithUuid map { f ⇒ q"${f.term} = mapper.${f.term}" }
      val fks = foreignKeys map { f ⇒
        q"""
        ${f.colName} =
          ${f.query}.idByUuid(mapper.${f.term}) getOrElse {
          uuidError(mapper, ${f.nameS}, Some(mapper.${f.term}))
        }
        """
      }
      val fields = att ++ fks
      val assocUpdates = assocs map { f ⇒
        q"""
        obj.${f.replaceMany}(${f.query}.idsByUuids(mapper.${f.term}))
        """
      }
      List(
        q"""
        def syncFromMapper(mapper: $mt)($session) {
          mapper.uuid.flatMap(idByUuid) map {
            id ⇒ updateFromMapper(id, mapper) } getOrElse {
            createFromMapper(mapper)
          }
        }
        """,
        q"""
        def updateFromMapper(id: Long, mapper: $mt)($session) {
          applyMapper(id, mapper, update)
        }
        """,
        q"""
        def createFromMapper(mapper: $mt)($session) {
          applyMapper(0, mapper, insert)
        }
        """,
        q"""
        def applyMapper(id: Long, mapper: $mt, app: $name ⇒ Any)
        ($session) {
          val obj = $term(..$fields, id = id)
          app(obj)
          ..$assocUpdates
        }
        """,
        q"""
        def uuidError(mapper: $mt, attr: String, uuid: Option[String]) = {
          throw new Exception(
            s"Invalid uuid found in mapper $$mapper for attr $$attr:" +
              uuid.getOrElse("none")
          )
        }
        """
    )
    }

    def encodeJson = {
      val ident = TermName(s"${name.toString}EncodeJson")
      val fks = foreignKeys map { a ⇒ (a.nameS, q"obj.${a.term}.uuid") }
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
      extends slick.db.BackendMapper[$name]
      """
    }

    lazy val mapper = s"${name}Mapper"

    lazy val mapperType = TypeName(mapper)

    lazy val mapperTerm = TermName(mapper)

    lazy val mapperParams = {
      val atts = attrsWithUuid map { _.valDef }
      val fks = foreignKeys map { a ⇒ q"val ${a.term}: String" }
      val ass = assocs map { a ⇒ q"val ${a.term}: Seq[String]" }
      atts ++ fks ++ ass
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
      val error = q"""
      "Error parsing json for Enumeration attr '" + $nameS + s"': "
      """
      val decoder =
        q"""
        {
          c ⇒ c.focus.string match {
            case Some(n) ⇒
              Try($name.withName(n)) match {
                case scala.util.Success(e) ⇒ DecodeResult.ok(e)
                case scala.util.Failure(e) ⇒
                  DecodeResult.fail($error + e, c.history)
              }
            case None ⇒
              DecodeResult.fail($error + "Invalid input '$${c.focus}'",
                c.history)
          }
        }
        """
      q"""
      implicit val $ident: CodecJson[$tpe] = CodecJson(
        (v: $name.$tpe) ⇒ jString(v.toString), $decoder
      )
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

  def dateTimeCodecJson = {
    val error = q""" "Error parsing json for DateTime: " """
    val decoder =
      q"""
      {
        c ⇒ c.focus.string match {
          case Some(s) ⇒
            Try(s.toLong) match {
              case scala.util.Success(d) ⇒ DecodeResult.ok(d.toDateTime)
              case scala.util.Failure(e) ⇒
                DecodeResult.fail($error + e, c.history)
            }
          case None ⇒
            DecodeResult.fail($error + "Invalid input '$${c.focus}'",
              c.history)
        }
      }
      """
    q"""
    implicit val dateTimeCodecJson: CodecJson[DateTime] = CodecJson(
      (dt: DateTime) ⇒ jString(dt.unix.toString), $decoder
    )
    """
  }

  override def dateTime = {
    super.dateTime ++ Seq(dateTimeCodecJson)
  }

  override def extraPre(classes: List[ModelSpec]): List[Tree] = {
    List(
      q"import argonaut._",
      q"import Argonaut._",
      q"import slick.db.Uuids._"
    )
  }

  override def extra(classes: List[ModelSpec]) = {
    val jsonCodecs = classes.map(_.jsonCodec).flatten
    val backendMappers = classes.map(_.backendMapper)
    List(
      q"import slick.db.PendingActionsSchema._",
      q"val pendingActions = PendingActionSet",
      q"val pendingMetadata = slick.db.PendingActionsSchema.metadata",
      q"val syncMetadata = ${syncMetadata(classes)}"
    ) ++ backendMappers ++ jsonCodecs
  }

  def syncMetadata(classes: List[ModelSpec]) = {
    val data = classes map { cls ⇒
      val meta = q"""
      slick.db.SyncTableMetadata(${cls.path}, ${cls.query})
      """
      (cls.path, meta)
    }
    q"slick.db.SyncSchemaMetadata(Map(..$data))"
  }

  override val extraBases = List(tq"slick.SyncSchemaBase")

  override def schemaSpec(comp: CompanionData)(implicit info: BasicInfo) =
    SchemaSpec.parse[SyncSchemaMacros](comp)
}
