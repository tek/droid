package slick

import scala.reflect.macros.whitebox.Context
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

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

  class Transformer(cls: TableSpec)
  {
    def model = {
      val valdefs = cls.valDefs
      val defdefs = cls.foreignKeys.map { field ⇒
        val first = TermName(field.option ? "firstOption" / "first")
        q"""
        def ${field.load}($session) =
          ${field.query}.filter { _.id === ${field.colId} }.$first
        """
      }
      val one2many = cls.assocs.map { f ⇒
        val plur = f.name.up
        val assocQuery = q"self.${cls.assocQuery(f)}"
        val otherQuery = q"self.${f.query}"
        val model = cls.assocModel(f)
        val myId = cls.colId
        val otherId = f.assocQueryColId
        val add = f.singularTerm.prefix("add")
        Seq(
          q"""
          def ${f.loadMany} = for {
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
            id flatMap { i ⇒ $assocQuery.insert($model(i, $otherId)) }
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
              val existing = (for { x ← $assocQuery } yield x.$otherId).list
              ids filter { i ⇒ !existing.contains(i) } foreach {
                $add
              }
            }
            """
          )
      }.flatten
      q"""
      case class ${cls.name}(..$valdefs)
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
        val assocQuery = q"self.${cls.assocQuery(f)}"
        val otherQuery = q"self.${f.query}"
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
      } flatten
      val tableId = cls.sqlTableId
      q"""
      class ${cls.tableName}(tag: Tag)
      extends Table[${cls.name}](tag, $tableId)
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
      extends ${cls.queryType}(tag ⇒ new ${cls.tableName}(tag))
      with ${cls.queryBase}
      {
        def path = ${cls.path}
        ..${cls.queryExtra}
      }
      """
    }

    def result: List[Tree] = model :: table :: query :: Nil
  }

  object Transformer
  {
    def apply(cls: TableSpec) = new Transformer(cls)
  }

  def transform(cls: TableSpec) = Transformer(cls).result

  def createMetadata(tables: List[TableSpec]) = {
    val data = tables map { t ⇒
      (t.path, q"db.TableMetadata(${t.path}, ${t.query})")
    }
    List(
      q""" val tables = List(..${data.unzip._2}) """,
      q""" val tableMap = Map(..$data) """,
      q""" val metadata = db.SchemaMetadata(tableMap) """
    )
  }

  val extraBases = List(tq"slick.SchemaBase")

  def extra(classes: List[ModelSpec]): List[Tree] = Nil

  def extraPre(classes: List[ModelSpec]): List[Tree] = Nil

  def schemaSpec(comp: CompanionData)(implicit info: BasicInfo):
  SchemaSpec[_ <: SchemaMacros] =
    SchemaSpec.parse[SchemaMacros](comp)

  def impl(cls: ClassData, comp: CompanionData) = {
    implicit val info = BasicInfo(comp)
    val schema = schemaSpec(comp)
    val models = schema.models
    val assoc = models flatMap(_.assocTables)
    val tables = models ++ assoc
    val database = tables.flatMap(transform)
    val enums = schema.enum
    val metadata = createMetadata(tables)
    q"""
    object ${comp.name}
    extends ..${comp.bases ++ extraBases}
    { self ⇒
      import scalaz._, Scalaz._
      import scala.slick.util.TupleMethods._
      import scala.slick.jdbc.JdbcBackend
      import scala.slick.driver.SQLiteDriver.simple._
      import Uuids._
      import java.sql.Timestamp
      import org.joda.time.DateTime

      ..${extraPre(models)}
      ..$dateTime
      ..$enums
      ..$database
      ..$metadata
      ..${extra(models)}
      ..${schema.misc}

      val dbLock = new Object
    }
    """
  }
}
