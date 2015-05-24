package slick

import scala.reflect.macros.whitebox.Context

class Schema
extends annotation.StaticAnnotation
{
  def macroTransform(annottees: Any*): Any = macro SchemaMacros.process
}

class SchemaMacros(val c: Context)
extends SchemaMacrosBase
{
  import c.universe._

  implicit object BasicEnumProcessor
  extends BasicEnumProcessor[SchemaMacros]

  implicit def tableOps(cls: TableSpec) = cls match {
    case m: ModelSpec ⇒ modelOps(m)
    case a: AssocSpec ⇒ assocOps(a)
  }

  implicit def modelOps(cls: ModelSpec) = new ModelOps(cls)
  implicit def assocOps(cls: AssocSpec) = new AssocOps(cls)

  class TableTransformer(cls: TableSpec)
  {
    def model = {
      val valdefs = cls.valDefs
      val defdefs = cls.foreignKeys.map { field ⇒
        val first = TermName(field.option ? "firstOption" / "first")
        List(
          q"""
          def ${field.load}($session) =
            ${field.query}.filter { _.id === ${field.colId} }
          """,
          q"""
          def ${field.term}($session) =
            ${field.load}.$first
          """
        )
      } flatten
      val one2many = cls.assocs.map { f ⇒
        val plur = f.name.up
        val assocQuery = q"${cls.assocQuery(f)}"
        val otherQuery = q"${f.query}"
        val model = cls.assocModel(f)
        val myId = cls.colId
        val otherId = f.assocQueryColId
        val add = f.singularTerm.prefix("add")
        Seq(
          q"""
          def ${f.loadMany}: Query[${f.tableType}, ${f.actualType}, Seq] = for
          {
            x ← $assocQuery
            if x.${cls.colId} === id
            y ← $otherQuery
            if x.${f.assocQueryColId} === y.id
          } yield y
          """,
          q"""
          def ${f.term}($session) = ${f.loadMany}.list
          """,
          q"""
          def $add($otherId: Long)($session) =
            $assocQuery.insert($model(id, $otherId))
            """,
            q"""
            def ${TermName("remove" + plur)}(ids: Traversable[Long])
            ($session) = {
              val assoc = for {
                x ← $assocQuery
                if x.$myId === id && x.$otherId.inSet(ids)
              } yield x
              assoc.delete
            }
            """,
            q"""
            def ${TermName("delete" + plur)}(ids: Traversable[Long])
            ($session) = {
              val other = for {
                x ← $otherQuery
                if x.id.inSet(ids)
              } yield x
              other.delete
              ${TermName("remove" + plur)}(ids)
            }
            """,
            q"""
            def ${TermName("replace" + plur)}(ids: Traversable[Long])
            ($session) = {
              val removals = for {
                x ← $assocQuery
                if x.$myId === id && !x.$otherId.inSet(ids)
              } yield x
              removals.delete
              val existing = (
                for {
                  x ← $assocQuery
                  if x.$myId === id
                } yield x.$otherId
              ).list
              ids filter { i ⇒ !existing.contains(i) } foreach {
                $add
              }
            }
            """
          )
      }.flatten
      q"""
      case class ${cls.tpe}(..$valdefs)
      extends ..${cls.modelBases}
      {
        ..$defdefs
        ..$one2many
        ..${cls.modelExtra}
      }
      """
    }

    def table = {
      val foreignKeys = cls.foreignKeys.map { _.fkDef }
      val one2many = cls.assocs.map { f ⇒
        val assocQuery = q"${cls.assocQuery(f)}"
        val otherQuery = q"${f.query}"
        val model = cls.assocModel(f)
        val myId = cls.colId
        val otherId = f.colId
        Seq(
          q"""
          def ${f.term} = for {
            x ← $assocQuery
            if x.${cls.colId} === id
            y ← $otherQuery
            if x.${f.assocQueryColId} === y.id
          } yield(y)
          """
        )
      }.flatten
      q"""
      class ${cls.tableType}(tag: Tag)
      extends Table[${cls.tpe}](tag, ${cls.tableName})
      with ..${cls.tableBases}
      {
        ..${cls.columns}
        ..$one2many
        ..$foreignKeys
        ${cls.times}
      }
      """
    }

    def query = {
      q"""
      object ${cls.term}
      extends ${cls.queryType}(tag ⇒ new ${cls.tableType}(tag))
      with ${cls.queryBase}
      {
        def path = ${cls.path}
        ..${cls.queryExtra}
      }
      """
    }

    def result: List[Tree] = model :: table :: query :: Nil
  }

  object TableTransformer
  {
    def apply(cls: TableSpec) = new TableTransformer(cls)
  }

  def transform(cls: TableSpec) = TableTransformer(cls).result

  def createMetadata(tables: List[TableSpec]) = {
    val data = tables map { t ⇒
      (t.tableName, q"slick.db.TableMetadata(${t.tableName}, ${t.query})")
    }
    q"""
    val metadata = slick.db.SchemaMetadata(Map(..$data))
    """
  }

  val extraBases = List(tq"slick.SchemaBase")

  def extra(classes: List[ModelSpec]): List[Tree] = Nil

  def extraPre(classes: List[ModelSpec]): List[Tree] = Nil

  def imports = {
    q"""
    import scalaz._, Scalaz._
    import scala.slick.util.TupleMethods._
    import scala.slick.jdbc.JdbcBackend
    import scala.slick.driver.SQLiteDriver.simple._
    import java.sql.Timestamp
    import com.github.nscala_time.time.Imports._
    """
  }

  def schemaSpec(comp: CompanionData)(implicit info: BasicInfo):
  SchemaSpec[_ <: SchemaMacros] =
    SchemaSpec.parse[SchemaMacros](comp)

  object SchemaTransformer
  extends Transformer
  {
    def apply(cls: ClassData, comp: CompanionData) = {
      implicit val info = BasicInfo(comp)
      val schema = schemaSpec(comp)
      val models = schema.models
      val assoc = models flatMap(_.assocTables)
      val tables = models ++ assoc
      val database = tables.flatMap(transform)
      val enums = schema.enum
      val metadata = createMetadata(tables)
      val body = q"""
        ..$imports
        ..${schema.imports}
        ..${extraPre(models)}
        ..$dateTime
        ..$enums
        ..$database
        ..$metadata
        ..${extra(models)}
        ..${schema.misc}
        val dbLock = new Object
      """
      val bases = comp.bases ++ extraBases
      (cls, comp.copy(body = body.children, bases = bases))
    }
  }

  val transformers = SchemaTransformer :: Nil
}
