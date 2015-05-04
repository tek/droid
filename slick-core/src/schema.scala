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

  def model(desc: ClsDesc, augment: Boolean = true, sync: Boolean = false)
  (implicit classes: List[ClsDesc]): ClassDef = {
    if (desc.part) desc.tree.asInstanceOf[ClassDef]
    else {
      val uuids = augment && desc.uuids
      val idval = listIf(augment)(q"val id: Option[Long] = None")
      val uuidval = listIf(uuids)(q"val uuid: Option[String] = None")
      val valdefs = desc.flat.map { it ⇒
          if (it.cse) {
            val tpt = if (it.option) {
              tq"Option[Long]"
            } else {
              tq"Long"
            }
            val ValDef(mod, nme, _, _) = it.tree
            val termName = TermName(nme.toString + "Id")
            q"val $termName:$tpt"
          } else
            it.tree.asInstanceOf[ValDef]
      }
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
        val plur = f.name.capitalize
        val col = columnId(singL)
        val query = desc.assocQuery(f)
        val model = desc.assocModel(f)
        val myId = desc.colId
        val otherId = columnId(sing)
        val add = TermName("add" + f.singularName.capitalize)
        Seq(
          q"""
          def ${f.loadMany} = for {
            x ← $query
            if x.${desc.colId} === id
            y ← ${f.query}
            if x.${f.queryColId} === y.id
          } yield y
          """,
          q"""
          def $add($otherId: Long)($session) =
            id flatMap { i ⇒ $query.insert($model(i, $otherId)) }
          """,
          q"""
          def ${TermName("remove" + plur)}(ids: Traversable[Long])
          ($session) = {
            val assoc = for {
              x ← $query
              if x.$myId === id && x.$otherId.inSet(ids)
            } yield x
            assoc.delete
          }
          """,
          q"""
          def ${TermName("delete" + plur)}(ids: Traversable[Long])
          ($session) = {
            val other = for {
              x ← ${f.query}
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
              x ← $query
              if x.$myId === id && !x.$otherId.inSet(ids)
            } yield x
            removals.delete
            val existing = (for { x ← $query } yield x.$otherId).list
            ids filter { i ⇒ !existing.contains(i) } foreach {
              $add
            }
          }
          """
        )
      } flatten
      val complete = listIf(sync) {
        q"""
        def completeSync()($session) {
          ${desc.query}.completeSync(this)
        }
        """
      }
      val bases = Seq(
        (augment ? tq"db.Model"),
        ((augment && desc.timestamps) ? tq"db.Timestamps"),
        (uuids ? tq"db.Uuids")
      ).flatten
      q"""
      case class ${TypeName(desc.name)}(..$idval, ..$valdefs,
        ..${desc.dateVals}, ..$uuidval)
      extends ..$bases
      {
        ..$defdefs
        ..$one2many
        ..$complete
      }
      """
    }
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

  def mkTimes(desc: ClsDesc, augment: Boolean = true): Tree = {
    val expr = {
      if (augment)
        if (desc.timestamps)
          s"""def * = (id.?, ${mkTilde(desc.flat)}, dateCreated, lastUpdated).shaped <> ({
          case (id, ${mkCase(desc.flat)}, dateCreated, lastUpdated) ⇒ ${desc.name}(id, ${mkCaseApply(desc.flat)}, dateCreated, lastUpdated)
        }, { x : ${desc.name} ⇒ Some((x.id, ${mkCaseUnapply(desc.flat)}, x.dateCreated, x.lastUpdated))
        })"""
        else
          s"""def * = (id.?, ${mkTilde(desc.flat)}).shaped <> ({
          case (id, ${mkCase(desc.flat)}) ⇒ ${desc.name}(id, ${mkCaseApply(desc.flat)})
        }, { x : ${desc.name} ⇒ Some((x.id, ${mkCaseUnapply(desc.flat)}))
        })"""
      else
      //s"""def * = (${mkTilde(desc.fields toList)}).shaped <> (${desc.name}.tupled, ${desc.name}.unapply _)"""
        s"""def * = (${mkTilde(desc.fields toList)}).shaped <> ({
          case (${mkCase(desc.flat)}) ⇒ ${desc.name}( ${mkCaseApply(desc.flat)})
        }, { x : ${desc.name} ⇒ Some((${mkCaseUnapply(desc.flat)}))
        })"""

    }
    c.parse(expr)
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

  def mkTable(desc: ClsDesc, augment: Boolean = true,
    sync: Boolean = false)
  (implicit classes: List[ClsDesc]): List[Tree] = {
    if (desc.part)
      List(desc.tree.asInstanceOf[ClassDef])
    else {
      val listVals = desc.listValDefs
      val foreignKeys = desc.foreignKeys.map {
        it ⇒
          val cls = classes.find(it.typeName == _.name).getOrElse(throw new Exception(s"Invalid foreign class ${it.name}:${it.typeName}"))
          c.parse( s"""def ${it.name} = foreignKey("${it.name.toLowerCase}", ${colIdName(it.name)}, ${objectName(it.typeName)})(_.id) """)
      }
      val assocs = desc.assocs.map { it ⇒
        val name = desc.assocName(it)
        new ClsDesc(name, Set(ENTITYDEF),
          ListBuffer(
            new FldDesc(decapitalize(desc.name), decapitalize(desc.name),
              desc.name, Set(FieldFlag.CASE), None,
              Some(desc), q"val ${TermName(decapitalize(desc.name))} = null"),
            new FldDesc(decapitalize(it.typeName),
              decapitalize(it.typeName), it.typeName, Set(FieldFlag.CASE),
              None, it.cls,
              q"val ${TermName(decapitalize(it.typeName))} = null")
            ),
          null, plural(decapitalize(name)))

      }
      val assocTables = assocs.flatMap { ass ⇒ mkTable(ass, false) }
      val idCol = listIf(augment) {
        q"""
        def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
        """
      }
      val uuidCol = listIf(desc.uuids) {
        q""" def uuid = column[String]("uuid") """
      }
      val defdefs = desc.flat.flatMap { v ⇒
        if (v.part) v.cls.get.fields.map(column)
        else List(column(v))
      }
      val one2many = desc.assocs.map { f ⇒
        val sing = f.typeName
        val singL = decapitalize(sing)
        val col = columnId(singL)
        val query = desc.assocQuery(f)
        val model = desc.assocModel(f)
        val myId = desc.colId
        val otherId = columnId(sing)
        Seq(
          q"""
          def ${f.term} = for {
            x ← self.$query
            if x.${desc.colId} === id
            y ← self.${f.query}
            if x.${f.queryColId} === y.id
          } yield(y)
          """
        )
      } flatten
      val times = mkTimes(desc, augment)
      val plur = plural(decapitalize(desc.name)).toLowerCase
      val bases = Seq(
        (augment ? tq"TableEx[${desc.typeName}]")
      ).flatten
      val table = q"""
      class ${desc.tableType}(tag: Tag)
      extends Table[${desc.typeName}](tag, $plur)
      with ..$bases
      {
        ..$idCol
        ..$uuidCol
        ..${desc.dateDefs}
        ..$defdefs
        ..$one2many
        ..$foreignKeys
        $times
      }
      """
      model(desc, augment, sync) :: table :: query(desc) :: assocTables
    }
  }

  def queryBase(desc: ClsDesc) = {
    val crud = desc.assoc ? tq"CrudCompat" / (
      desc.timestamps ? tq"CrudEx" / tq"Crud"
    )
    tq"$crud[${desc.typeName}, ${desc.tableType}]"
  }

  def queryType(desc: ClsDesc): Tree = tq"TableQuery"

  def query(desc: ClsDesc) = {
    val tp = queryType(desc)
    val base = queryBase(desc)
    q"""
    val ${desc.query} =
      new $tp(tag ⇒ new ${desc.tableType}(tag)) with $base
      {
        def name = ${desc.query.toString}
      }
    """
  }

  def defMap(body: List[c.universe.Tree]): Map[DefType, List[(DefType, c.universe.Tree)]] = {
    body.flatMap {
      it ⇒
        it match {
          case ModuleDef(mod, name, Template(List(Ident(t)), _, _)) if t.isTypeName && t.toString == "Enumeration" ⇒ List((ENUMDEF, it))
          case ClassDef(mod, name, Nil, body) if mod.hasFlag(Flag.CASE) ⇒ List((CLASSDEF, it))
          case DefDef(_, _, _, _, _, _) ⇒ List((DEFDEF, it))
          case Import(_, _) ⇒ List((IMPORTDEF, it))
          case _ ⇒ List((OTHERDEF, it))
        }
    } groupBy (_._1)
  }

  def enum(tree: Tree) = {
    val q"object $name extends ..$bases { ..$body }" = tree
    val nameS = name.toString
    val tpe = TypeName(nameS)
    val ident = TermName(s"enum${name}Codec")
    val json = q"""
    implicit val $ident = jencode1L { v: $name.$tpe ⇒ v.toString } ($nameS)
    """
    val mapper = mkEnumMapper(tree)
    List(tree, mapper, json)
  }

  def jsonCodec(cls: ClsDesc) = {
    val name = TypeName(cls.name)
    val ident = TermName(s"${cls.name}JsonCodec")
    val encoder = TermName(s"jencode${cls.fields.length}L")
    val fks = cls.foreignKeys map { a ⇒ (a.name, q"obj.${a.load}.uuid") }
    val assocs = cls.assocs map { a ⇒
      (a.name, q"obj.${a.loadMany}.list.uuids")
    }
    val attrs = cls.attrs map { a ⇒ (a.name, q"obj.${a.term}") }
    val (names, values) = (fks ++ assocs ++ attrs).unzip
    q"""
    implicit def $ident ($session) =
      $encoder { obj: $name ⇒
        (..$values) }(..$names)
    """
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

  def impl(name: TermName, bases: List[Tree], body: List[Tree]) = {
    val parents = bases map { _.toString.split('.').last }
    val timestampsAll = parents.contains("Timestamps")
    val uuids = parents.contains("Uuids")
    val sync = parents.contains("Sync")
    val allDefs = defMap(body)
    implicit val classes = allDefs.getOrElse(CLASSDEF, Nil).map(it ⇒
        ClsDesc(it._2, timestampsAll, uuids))
    classes.foreach(_.parseBody(classes))
    val tables = classes.flatMap(mkTable(_, true, sync))
    val enums = allDefs.getOrElse(ENUMDEF, Nil).map(_._2) flatMap(enum)
    val defdefs = allDefs.getOrElse(DEFDEF, Nil).map(_._2)
    val imports = allDefs.getOrElse(IMPORTDEF, Nil).map(_._2)
    val otherdefs = allDefs.getOrElse(OTHERDEF, Nil).map(_._2)
    val embeds = allDefs.getOrElse(EMBEDDEF, Nil).map(_._2)
    val jsonCodecs = if (uuids) classes map(jsonCodec) else Nil
    val metadata = createMetadata
    val result = q"""
    object $name extends ..${bases ++ extraBases} { self ⇒
      import scala.slick.util.TupleMethods._
      import scala.slick.jdbc.JdbcBackend
      import scala.slick.driver.SQLiteDriver.simple._
      import slick._
      import slick.db._
      import Uuids._
      import argonaut._
      import Argonaut._
      import java.sql.Timestamp
      import org.joda.time.DateTime

      ..$dateTime
      ..$enums
      ..$imports
      ..$embeds
      ..$tables
      ..$jsonCodecs
      ..$metadata
      ..$extra

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
