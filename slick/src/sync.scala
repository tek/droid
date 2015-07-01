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

  class SyncModelOps(m: ModelSpec)
  extends ModelOps(m)
  {
    val idType = tq"ObjectId"

    lazy val mapperFields = attrs ++ foreignKeys ++ assocs :+ idColumn

    lazy val mapperFieldStrings = mapperFields.map(_.name.toString)

    def attrsWithId = attrs ++ foreignKeys :+ idColumn

    override def modelBases = super.modelBases :+ tq"slick.db.Sync"

    override def tableBases = List(tq"slick.db.SyncTable[$tpe]")

    override def queryBase = tq"slick.Empty"

    override def queryType =
      tq"slick.db.SyncTableQuery[$tpe, $tableType, $mapperType]"

    override def queryExtra = {
      super.queryExtra ++ List(
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
      val fields = attrsWithId map { f ⇒ q"${f.colName} = mapper.${f.term}" }
      val assocUpdates = assocs map { f ⇒
        q"obj.${f.replace}(mapper.${f.term})"
      }
      List(
        q"""
        def syncFromMapper(mapper: $mt)($session) {
          if (idExists(mapper.id)) updateFromMapper(mapper)
          else createFromMapper(mapper)
        }
        """,
        q"""
        def updateFromMapper(mapper: $mt)($session) {
          applyMapper(mapper, updateUnrecorded)
        }
        """,
        q"""
        def createFromMapper(mapper: $mt)($session) {
          applyMapper(mapper, insertUnrecorded)
        }
        """,
        q"""
        def applyMapper(mapper: $mt, app: $tpe ⇒ Option[$tpe])
        ($session) {
          app($term(..$fields)) foreach { obj ⇒
            ..$assocUpdates
          }
        }
        """,
        q"""
        def objectIdError(mapper: $mt, attr: String, objectId: Option[String]) = {
          throw new Exception(
            s"Invalid objectId found in mapper $$mapper for attr $$attr:" +
              objectId.getOrElse("none")
          )
        }
        """
    )
    }

    def encodeJson = {
      val ident = TermName(s"${name}EncodeJson")
      val ass = assocs map { a ⇒
        (a.nameS, q"obj.${a.ids}")
      }
      val att = attrsWithId map { a ⇒ (a.nameS, q"obj.${a.colName}") }
      val (names, values) = (ass ++ att).unzip
      val encoder = TermName(s"jencode${values.length}L")
      q"""
      implicit def $ident ($session) =
        $encoder { obj: $tpe ⇒ (..$values) }(..$names)
      """
    }

    def mapperCodec = {
      val ident = TermName(s"${name}MapperCodecJson")
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
      extends slick.db.BackendMapper[$tpe]
      """
    }

    lazy val mapper = s"${name}Mapper"

    lazy val mapperType = TypeName(mapper)

    lazy val mapperTerm = TermName(mapper)

    lazy val mapperParams = mapperFields map { _.mapperParam }

    override def updatedHook = q"""
    def updatedHook()($session) = {
      $term.recordUpdate(id)
    }
    """
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

  class SyncTableTransformer(cls: TableSpec)
  extends TableTransformer(cls)
  {

  }

  object SyncTableTransformer
  {
    def apply(cls: TableSpec) = new SyncTableTransformer(cls)
  }

  override def transform(cls: TableSpec) = SyncTableTransformer(cls).result

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

  def objectIdCodecJson = {
    val error = q""" "Error parsing json for ObjectId: " """
    val decoder =
      q"""
      {
        c ⇒ c.focus.string match {
          case Some(s) ⇒
              DecodeResult.ok(new $idType(s))
          case None ⇒
            DecodeResult.fail($error + "Invalid input '$${c.focus}'",
              c.history)
        }
      }
      """
    q"""
    implicit val objectIdCodecJson: CodecJson[$idType] = CodecJson(
      (id: $idType) ⇒ jString(id.toString), $decoder
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
      q"import slick.db.HasObjectId._"
    )
  }

  override def extra(classes: List[ModelSpec]) = {
    val jsonCodecs = classes.map(_.jsonCodec).flatten
    val backendMappers = classes.map(_.backendMapper)
    List(
      q"import slick.db.PendingActionsSchema._",
      q"val pendingActions = PendingActionSet",
      q"val pendingMetadata = slick.db.PendingActionsSchema.metadata",
      q"val syncMetadata = ${syncMetadata(classes)}",
      q"""
      def initPending()($session) =
        syncMetadata.tables mapValues { _.table.pending }
      """,
      q"""
      def initDb()($session) = {
        metadata.createMissingTables()
        pendingMetadata.createMissingTables()
        initPending()
      }
      """,
      q"""
      def dropDb()($session) = {
        metadata.dropAll()
        pendingMetadata.dropAll()
      }
      """
    ) ++ backendMappers ++ Seq(objectIdCodecJson) ++ jsonCodecs
  }

  def syncMetadata(classes: List[ModelSpec]) = {
    val data = classes map { cls ⇒
      val meta = q"""
      slick.db.SyncTableMetadata(${cls.tableName}, ${cls.query})
      """
      (cls.path, meta)
    }
    q"slick.db.SyncSchemaMetadata(Map(..$data))"
  }

  override val extraBases = List(tq"slick.SyncSchemaBase")

  override def schemaSpec(comp: CompanionData)(implicit info: BasicInfo) =
    SchemaSpec.parse[SyncSchemaMacros](comp)
}
