package slick

import scala.reflect.macros.whitebox.Context
import scala.annotation.StaticAnnotation
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

class Schema
extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro SchemaMacros.create
}

class SchemaMacros(val c: Context)
extends SchemaMacrosBase
{
  import c.universe._
  import SchemaHelpers._
  import DefType._
  import ClassFlag._
  import FieldFlag._
  import Helpers._
  import Flag._

  def extra(implicit classes: List[ClsDesc]): List[Tree] = Nil

  def extraPre(implicit classes: List[ClsDesc]): List[Tree] = Nil

  def model(desc: ClsDesc, augment: Boolean = true) = {
    val idval = listIf(augment)(q"val id: Option[Long] = None")
    val valdefs = desc.caseValDefs
    val defdefs = desc.foreignKeys.map { field ⇒
      val first = TermName(field.option ? "firstOption" / "first")
      q"""
      def ${field.load}($session) =
        ${field.query}.filter { _.id === ${field.colId} }.$first
      """
    }
    val one2many = desc.assocs.map { f ⇒
      val sing = f.typeName
      val singL = decapitalize(sing)
      val plur = plural(f.name.capitalize)
      val col = columnId(singL)
      val query = desc.assocQuery(f)
      val model = desc.assocModel(f)
      val myId = desc.colId
      val otherId = columnId(sing)
      val add = TermName("add" + f.singularName.capitalize)
      Seq(
        q"""
        def ${f.loadMany} = for {
          x ← self.$query
          if x.${desc.colId} === id
          y ← self.${f.query}
          if x.${f.queryColId} === y.id
        } yield y
        """,
        q"""
        def ${f.term}($session) = ${f.loadMany}.list
        """,
        q"""
        def $add($otherId: Long)($session) =
          id flatMap { i ⇒ self.$query.insert($model(i, $otherId)) }
        """,
        q"""
        def ${TermName("remove" + plur)}(ids: Traversable[Long])
        ($session) = {
          val assoc = for {
            x ← self.$query
            if x.$myId === id && x.$otherId.inSet(ids)
          } yield x
          assoc.delete
        }
        """,
        q"""
        def ${TermName("delete" + plur)}(ids: Traversable[Long])
        ($session) = {
          val other = for {
            x ← self.${f.query}
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
            x ← self.$query
            if x.$myId === id && !x.$otherId.inSet(ids)
          } yield x
          removals.delete
          val existing = (for { x ← self.$query } yield x.$otherId).list
          ids filter { i ⇒ !existing.contains(i) } foreach {
            $add
          }
        }
        """
      )
    } flatten
    val bases = Seq(
      (augment ? tq"db.Model"),
      ((augment && desc.timestamps) ? tq"db.Timestamps")
    ).flatten ++ desc.bases
    q"""
    case class ${TypeName(desc.name)}(..$idval, ..$valdefs,
      ..${desc.dateVals})
    extends ..$bases
    {
      ..$defdefs
      ..$one2many
    }
    """
  }

  def column(desc: FldDesc): Tree = {
    val q"$mods val $nme:$tpt = $initial" = desc.tree
    if (desc.cse) {
      val tp = desc.option ? tq"Option[Long]" / tq"Long"
      q"""def ${desc.colId} = column[$tp](${desc.sqlColId})"""
    } else {
      val fields = q"${desc.colName}" ::
        { desc.dbType map(a ⇒ List(q"O.DBType(${a})")) getOrElse Nil }
      q"def $nme = column[$tpt](..$fields)"
    }
  }

  def concatFields(fields: List[FldDesc], prefix: String = "")
  (formatter: (FldDesc) ⇒ String) = {
    fields map { field ⇒
      if (field.part)
        formatter(field)
      else if (field.cse)
        prefix + colIdName(field.name)
      else
        prefix + field.name
    } mkString(", ")
  }

  def mkTilde(fields: List[FldDesc]): String = concatFields(fields) { f ⇒
    "(" + mkTilde(f.cls.get.fields toList) + ")"
  }

  def mkCaseApply(fields: List[FldDesc]) = concatFields(fields) { f ⇒
    s"${f.typeName}.tupled.apply(${f.name})"
  }

  def mkCaseUnapply(fields: List[FldDesc]) =
    concatFields(fields, "x.") { f ⇒
      s"${f.typeName}.unapply(x.${f.name}).get"
    }

  def mkCase(fields: List[FldDesc]) = concatFields(fields) { _.name }

  def times(cls: ClsDesc, augment: Boolean) = {
    val tilde = cls.tildeFields(augment)
    val args = cls.modelFields(augment)
    val patParams = args map { a ⇒ pq"$a" }
    val pattern = pq"""(..$patParams)"""
    val idqm = augment ? List(q"id.?") / Nil
    val tupleArgs = cls.modelFieldsPrefixed(augment, "a")
    q"""
    def * = (..$idqm, ..$tilde).shaped <> ((${cls.compName}.apply _).tupled,
      ${cls.compName}.unapply)
    """
  }

  def mkEnumMapper(moduleDef: c.universe.Tree): Tree = {
    val ModuleDef(_, name, Template(_, _, defs)) = moduleDef
    val res = defs.flatMap {
      it ⇒
        it match {
          case ValDef(_, _, _, Apply(Ident(valueKeyword), List(Literal(Constant(value: Int))))) ⇒ Some("Int")
          case ValDef(_, _, _, Apply(Ident(valueKeyword), List(Literal(Constant(value: String))))) ⇒ Some("String")
          case ValDef(_, _, _, Apply(Ident(valueKeyword), _)) ⇒ Some("Int")
          case _ ⇒ None
        }
    } toSet

    val valueType = res.headOption.getOrElse("Int")

    val mapper = valueType match {
      case "Int" ⇒
        s"""implicit val ${name.toString}TypeMapper = MappedColumnType.base[${name.toString}.Value, $valueType](
          {
            it ⇒ it.id
          },
          {
            id ⇒ ${name}(id)
          })"""
      case "String" ⇒
        s"""implicit val ${name.toString}TypeMapper = MappedColumnType.base[${name.toString}.Value, $valueType](
          {
            it ⇒ it.toString
          },
          {
            id ⇒ ${name.toString}.withName(id)
          })"""

    }
    c.parse(mapper)
  }

  def tableBase(cls: ClsDesc) = tq"TableEx[${cls.typeName}]"

  def table(cls: ClsDesc, augment: Boolean = true)
  (implicit classes: List[ClsDesc]) =
  {
    val listVals = cls.listValDefs
    val foreignKeys = cls.foreignKeys.map { fk ⇒
      val cls = classes.find(fk.typeName == _.name).getOrElse(throw new Exception(s"Invalid foreign class ${fk.name}:${fk.typeName}"))
      c.parse( s"""def ${fk.name} = foreignKey("${fk.name.toLowerCase}", ${colIdName(fk.name)}, ${objectName(fk.typeName)})(_.id) """)
    }
    val idCol = listIf(augment) {
      q"""
      def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
      """
    }
    val defdefs = cls.flat.flatMap { v ⇒
      if (v.part) v.cls.get.fields.map(column)
      else List(column(v))
    }
    val one2many = cls.assocs.map { f ⇒
      val sing = f.typeName
      val singL = decapitalize(sing)
      val col = columnId(singL)
      val query = cls.assocQuery(f)
      val model = cls.assocModel(f)
      val myId = cls.colId
      val otherId = columnId(sing)
      Seq(
        q"""
        def ${f.term} = for {
          x ← self.$query
          if x.${cls.colId} === id
          y ← self.${f.query}
          if x.${f.queryColId} === y.id
        } yield(y)
        """
      )
    } flatten
    val tim = times(cls, augment)
    val plur = plural(decapitalize(cls.name)).toLowerCase
    val bases = listIf(augment)(tableBase(cls))
    q"""
    class ${cls.tableType}(tag: Tag)
    extends Table[${cls.typeName}](tag, $plur)
    with ..$bases
    {
      ..$idCol
      ..${cls.dateDefs}
      ..$defdefs
      ..$one2many
      ..$foreignKeys
      $tim
    }
    """
  }

  def assocTables(cls: ClsDesc)(implicit classes: List[ClsDesc]) = {
    cls.assocs.flatMap { ass ⇒
      val name = cls.assocName(ass)
      val assocCls = new ClsDesc(name, Set(ENTITYDEF),
        ListBuffer(
          new FldDesc(decapitalize(cls.name), decapitalize(cls.name),
            cls.name, Set(FieldFlag.CASE), None,
            Some(cls), q"val ${TermName(decapitalize(cls.name))} = null"),
          new FldDesc(decapitalize(ass.typeName),
            decapitalize(ass.typeName), ass.typeName, Set(FieldFlag.CASE),
            None, ass.cls,
            q"val ${TermName(decapitalize(ass.typeName))} = null")
          ),
        null, plural(decapitalize(name)))
      transform(assocCls, false)
    }
  }

  def transform(cls: ClsDesc, augment: Boolean = true)
  (implicit classes: List[ClsDesc]): List[Tree] =
  {
    if (cls.part)
      List(cls.tree.asInstanceOf[ClassDef])
    else {
      model(cls, augment) :: table(cls, augment) :: query(cls) ::
        assocTables(cls)
    }
  }

  def queryBase(cls: ClsDesc) = {
    val crud = cls.assoc ? tq"CrudCompat" / (
      cls.timestamps ? tq"CrudEx" / tq"Crud"
    )
    tq"$crud[${cls.typeName}, ${cls.tableType}]"
  }

  def queryType(cls: ClsDesc): Tree = tq"TableQuery"

  def queryExtra(cls: ClsDesc): List[Tree] = Nil

  def query(cls: ClsDesc) = {
    val tp = queryType(cls)
    val base = queryBase(cls)
    val extra = queryExtra(cls)
    q"""
    val ${cls.query} =
      new $tp(tag ⇒ new ${cls.tableType}(tag)) with $base
      {
        def name = ${cls.query.toString}
        ..$extra
      }
    """
  }

  def defMap(body: List[c.universe.Tree]): Map[DefType, List[(DefType, c.universe.Tree)]] = {
    body.flatMap {
      it ⇒
        it match {
          case ModuleDef(mod, name, Template(List(Ident(t)), _, _)) if t.isTypeName && t.toString == "Enumeration" ⇒ List((ENUMDEF, it))
          case ClassDef(mod, name, Nil, body) if mod.hasFlag(Flag.CASE) ⇒ List((CLASSDEF, it))
          case q"implicit class $name(..$params) { ..$body }" ⇒ List((DEFDEF, it))
          case DefDef(_, _, _, _, _, _) ⇒ List((DEFDEF, it))
          case Import(_, _) ⇒ List((IMPORTDEF, it))
          case _ ⇒ List((OTHERDEF, it))
        }
    } groupBy (_._1)
  }

  def enum(tree: Tree) = {
    List(tree, mkEnumMapper(tree))
  }

  def createMetadata(implicit classes: List[ClsDesc]) = {
    val data = classes flatMap { cls ⇒
      cls.queries map { name ⇒
        (name, q"db.TableMetadata(${name}, ${TermName(name)})")
      }
    }
    List(
      q""" val tables = List(..${data.unzip._2}) """,
      q""" val tableMap = Map(..$data) """,
      q""" val metadata = db.SchemaMetadata(tableMap) """
    )
  }

  val extraBases = List(tq"slick.SchemaBase")

  def createClsDesc(tree: Tree, timestamps: Boolean) = {
    ClsDesc(tree, timestamps, false)
  }

  def impl(name: TermName, bases: List[Tree], body: List[Tree]) = {
    val parents = bases map { _.toString.split('.').last }
    val timestamps = parents.contains("Timestamps")
    val allDefs = defMap(body)
    implicit val classes =
      allDefs.getOrElse(CLASSDEF, Nil).map(d ⇒ createClsDesc(d._2, timestamps))
    classes.foreach(_.parseBody(classes))
    val database = classes.flatMap(transform(_, true))
    val enums = allDefs.getOrElse(ENUMDEF, Nil).map(_._2) flatMap(enum)
    val defdefs = allDefs.getOrElse(DEFDEF, Nil).map(_._2)
    val imports = allDefs.getOrElse(IMPORTDEF, Nil).map(_._2)
    val otherdefs = allDefs.getOrElse(OTHERDEF, Nil).map(_._2)
    val embeds = allDefs.getOrElse(EMBEDDEF, Nil).map(_._2)
    val metadata = createMetadata
    val result = q"""
    object $name extends ..${bases ++ extraBases} { self ⇒
      import scalaz._, Scalaz._
      import scala.slick.util.TupleMethods._
      import scala.slick.jdbc.JdbcBackend
      import scala.slick.driver.SQLiteDriver.simple._
      import Uuids._
      import java.sql.Timestamp
      import org.joda.time.DateTime

      ..$extraPre
      ..$dateTime
      ..$enums
      ..$imports
      ..$embeds
      ..$database
      ..$metadata
      ..$defdefs
      ..$extra
      ..$otherdefs

      val dbLock = new Object
    }
    """
    val pw = new java.io.PrintWriter(
      new java.io.File(s"./target/slick-$name.scala"))
    pw.write(result.toString)
    pw.close
    c.Expr(result)
  }
}
