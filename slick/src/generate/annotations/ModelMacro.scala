package slickmacros.annotations

import scala.reflect.macros.whitebox.Context
import scala.annotation.StaticAnnotation
import scala.language.existentials
import language.experimental.macros
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import scala.slick.model.ForeignKeyAction
import slickmacros._

class Slick()
extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro SlickMacro.impl
}

trait Timestamps

trait Part

trait Uuids

object SlickMacro
{
  object DefType extends Enumeration {
    type DefType = Value
    val CLASSDEF = Value
    val DEFDEF = Value
    val IMPORTDEF = Value
    val ENUMDEF = Value
    val EMBEDDEF = Value
    val OTHERDEF = Value
  }

  object ClassFlag extends Enumeration {
    type ClassFlag = Value
    val PARTDEF = Value
    val ENTITYDEF = Value
    val TIMESTAMPSDEF = Value
    val UUIDSDEF = Value
    val OTHER = Value
  }

  object FieldFlag extends Enumeration {
    type FieldFlag = Value
    val OPTION = Value
    val CASE = Value
    val PART = Value
    val LIST = Value
  }
}

class SlickMacro(val c: Context)
{
  import c.universe._
  import SlickMacro._
  import DefType._
  import ClassFlag._
  import FieldFlag._
  import Helpers._
  import Flag._

  def listIf(indicator: Boolean)(ctor: ⇒ Tree): List[Tree] = {
    if (indicator) List(ctor)
    else List[Tree]()
  }

  def listIfList(indicator: Boolean)(ctor: ⇒ List[Tree]): List[Tree] = {
    if (indicator) ctor
    else List[Tree]()
  }

  val session = q"implicit val session: JdbcBackend#SessionDef"

  def constraints(plural: String = null)(f: ⇒ Unit) {}

  lazy val dateTime = {
    Seq(
      q"""
      implicit val DateTimeTypeMapper =
        MappedColumnType.base[DateTime, Timestamp](
        { dt ⇒ new Timestamp(dt.getMillis) },
        { ts ⇒ new DateTime(ts.getTime) }
      )
      """,
      q"""
      implicit val dateTimeJsonFormat =
        jencode1L { (dt: DateTime) ⇒ dt.getEra } ("time")
      """
    )
  }

  val reservedNames = List("id", "dateCreated", "lastUpdated", "uuid")

  def decapitalize(name: String): String = {
    if (name == null || name.length == 0) {
      name;
    } else {
      val chars = name.toCharArray()
      var i = 0
      while (i < chars.length && Character.isUpperCase(chars(i))) {
        if (i > 0 && i < chars.length - 1 && Character.isLowerCase(chars(i + 1))) {

        } else {
          chars(i) = Character.toLowerCase(chars(i))
        }
        i = i + 1
      }
      new String(chars)
    }
  }

  def mkTableName(typeName: String) = s"${typeName}Table"

  def assocTableName(table1: String, table2: String) = s"${table1}2${table2}"

  def sqlColName(name: String): String = {
    name.toCharArray().zipWithIndex map {
      case (ch, i) if Character.isUpperCase(ch) && i > 0 ⇒
        "_" + Character.toLowerCase(ch)
      case (ch, _) ⇒ Character.toLowerCase(ch)
    } mkString
  }

  def columnId(name: String) = {
    TermName(colIdName(name))
  }

  def colIdName(name: String) = {
    s"${decapitalize(name)}Id"
  }

  def objectName(typeName: String) = plural(decapitalize(typeName))

  case class FldDesc(
    name: String,
    colName: String,
    typeName: String,
    flags: Set[FieldFlag],
    dbType: Option[String],
    cls: Option[ClsDesc],
    tree: Tree
  ) {
    lazy val part = flags.contains(FieldFlag.PART)

    lazy val option = flags.contains(FieldFlag.OPTION)

    lazy val cse = flags.contains(FieldFlag.CASE)

    lazy val term = TermName(name)

    lazy val load = TermName(s"load${name.capitalize}")

    lazy val query = TermName(objectName(typeName))

    lazy val colId = columnId(name)

    lazy val sqlColId = sqlColName(colIdName(name))

    lazy val queryColId = columnId(typeName)
  }

  object FldDesc {
    def apply(fieldTree: Tree, clsTree: Tree, allClasses: List[ClsDesc]) = {
      val ValDef(mod, name, tpt, rhs) = fieldTree
      if (reservedNames.contains(name.toString))
        c.abort(c.enclosingPosition,
          s"Column with name ${name.toString} not allowed")
      val flags = Set[FieldFlag]()
      val annotation = mod.annotations.headOption.map(_.children.head.toString)
      var colName: String = sqlColName(name.toString)

      def buildTypeName(tree: Tree): String = {
        tree match {
          case Select(subtree, name) ⇒
            buildTypeName(subtree) + "." + name.toString
          case AppliedTypeTree(subtree, args) ⇒
            buildTypeName(subtree) + "[" + args.map(it ⇒ buildTypeName(it)).mkString(",") + "]"
          case Ident(x) ⇒
            x.toString
          case other ⇒ other.toString
        }
      }
      var typeName: String = buildTypeName(tpt)
      val clsDesc: Option[ClsDesc] = tpt match {
        case Ident(tpe) ⇒
          val clsDesc = allClasses.find(_.name == typeName)
          clsDesc.foreach {
            it ⇒
              if (it.entity) {
                flags += FieldFlag.CASE
              } else if (it.part)
                flags += FieldFlag.PART
          }
          clsDesc
        case AppliedTypeTree(Ident(option), tpe :: Nil) if option.toString == "Option" ⇒
          typeName = buildTypeName(tpe)
          flags += FieldFlag.OPTION
          val clsDesc = allClasses.find(_.name == typeName)
          clsDesc.foreach {
            it ⇒
              if (it.entity)
                flags += FieldFlag.CASE
          }
          clsDesc
        case AppliedTypeTree(Ident(list), tpe :: Nil) if list.toString == "List" ⇒
          typeName = buildTypeName(tpe)
          val clsDesc = allClasses.find(_.name == typeName).getOrElse(c.abort(c.enclosingPosition, s"List not allowed here ${name.toString} not allowed"))

          if (clsDesc.entity)
            flags ++= Set(FieldFlag.CASE, FieldFlag.LIST)
          else
            c.abort(c.enclosingPosition, s"only entity allowed here ${name.toString}:${clsDesc.name}")
          Some(clsDesc)
        case _ ⇒ None
      }
      val ClassDef(_, clsName, _, Template(_, _, body)) = clsTree
      body.foreach {
        it ⇒
          val cns = it match {
            case Apply(Ident(_), List(Block(stats, expr))) ⇒
              Some(plural(decapitalize(clsName.toString)), stats :+ expr)
            case Apply(Apply(Ident(_), List(Literal(Constant(arg)))),
              List(Block(stats, expr))) ⇒
              Some(arg.toString, stats :+ expr)
            case _ ⇒ None
          }
          cns foreach {
            it ⇒
              allClasses.find(_.name == clsName.toString).foreach {
                x ⇒
                  x.plural = it._1
              }
              (it._2).foreach {
                s ⇒
                  val st = s.toString.replace("scala.Tuple", "Tuple").split('.').map(_.trim)
                  if (st.length >= 2) {
                    val fieldNames = {
                      if (st(0).endsWith(")")) {
                        st(0).substring(st(0).indexOf('(') + 1, st(0).lastIndexOf(')')).split(',').map(_.trim)
                      } else {
                        Array(st(0).trim)
                      }
                    }
                  }
              }
          }
      }
      new FldDesc(name.toString, colName, typeName, flags,
        None, clsDesc, fieldTree)
    }
  }

  case class ClsDesc(name: String, flags: Set[ClassFlag],
    fields: ListBuffer[FldDesc], tree: Tree, var plural: String)
  {
    def parseBody(allClasses: List[ClsDesc]) {
      val ClassDef(mod, name, Nil, Template(parents, self, body)) = tree
      body.foreach {
        it ⇒
          it match {
            case ValDef(_, _, _, _) ⇒ fields += FldDesc(it, tree, allClasses)
            case _ ⇒
          }
      }
      if (fields.isEmpty)
        c.abort(c.enclosingPosition, "Cannot create table with zero column")
    }

    lazy val assoc = tree == null

    lazy val part = flags.contains(PARTDEF)

    lazy val entity = flags.contains(ENTITYDEF)

    lazy val timestamps = flags.contains(TIMESTAMPSDEF)

    lazy val uuids = flags.contains(UUIDSDEF)

    lazy val dateVals = listIfList(timestamps) {
      List(
        q" var dateCreated: DateTime = null",
        q" var lastUpdated: DateTime = null"
        )
    }

    lazy val dateDefs = listIfList(timestamps) {
      List(
        q""" def dateCreated = column[DateTime]("date_created") """,
        q""" def lastUpdated = column[DateTime]("last_updated") """
        )
      }

    lazy val assocNames = {
      assocs map { ass ⇒ assocTableName(name, ass.typeName) }
    }

    lazy val names = name :: assocNames

    lazy val queries = names map { n ⇒ Helpers.plural(decapitalize(n)) }

    lazy val tableName = mkTableName(name)

    lazy val tableType = TypeName(mkTableName(name))

    lazy val obj = TermName(objectName(name))

    lazy val typeName = TypeName(name)

    def assocName(other: String) =
      TermName(objectName(assocTableName(name, other)))

    def filter(pred: Set[FieldFlag] ⇒ Boolean) =
      fields.filter(f ⇒ pred(f.flags)).toList

    lazy val foreignKeys = filter { f ⇒
      f.contains(FieldFlag.CASE) && !f.contains(FieldFlag.LIST)
    }

    lazy val assocs = filter { f ⇒
      f.contains(FieldFlag.CASE) && f.contains(FieldFlag.LIST)
    }

    lazy val attrs = filter { !_.contains(FieldFlag.CASE) }

    lazy val flat = filter { !_.contains(FieldFlag.LIST) }

    lazy val listValDefs = filter { _.contains(FieldFlag.LIST) }

    def allFields = {
      fields.toList.map {
        it ⇒
          if (it.part)
            it.cls.get.fields toList
          else
            it :: Nil
      } flatten
    }

    lazy val colId = columnId(name)
  }

  object ClsDesc {
    def apply(tree: Tree, timestampAll: Boolean, uuids: Boolean) = {
      val q"case class $name(..$fields) extends ..$bases { ..$body }" =
        tree
      val parents = bases map { _.toString }
      val isPart = parents.contains("Part")
      val timestamps = parents.contains("Timestamps")
      val flags = Set[ClassFlag]()
      if (isPart)
        flags += PARTDEF
      else
        flags += ENTITYDEF
      if (timestampAll || timestamps) flags += TIMESTAMPSDEF
      if (uuids) flags += UUIDSDEF
      new ClsDesc(name.toString, flags, ListBuffer(), tree,
        plural(decapitalize(name.toString)))
    }
  }

  def impl(annottees: c.Expr[Any]*) = {

    def model(desc: ClsDesc, augment: Boolean = true)
    (implicit classes: List[ClsDesc]): ClassDef = {
      if (desc.part) desc.tree.asInstanceOf[ClassDef]
      else {
        val uuids = augment && desc.uuids
        val idval = listIf(augment)(q"val id: Option[Long] = None")
        val uuidval = listIf(uuids)(q"val uuid: Option[String] = None")
        val xid = listIf(augment) {
          q"""
          def xid = id.getOrElse(throw new Exception("Object has no id yet"))
          """
        }
        val valdefs = desc.flat.map {
          it ⇒
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
        val one2manyDefs = desc.assocs.map { f ⇒
          q"""
          def ${f.load} = for {
            x ← self.${desc.assocName(f.typeName)}
            if x.${desc.colId} === id
            y ← self.${f.query}
            if x.${f.queryColId} === y.id
          } yield(y)
          """
        }
        val one2manyDefAdds = desc.assocs.map { f ⇒
          val sing = f.typeName
          val singL = decapitalize(sing)
          Seq(
            q"""
            def ${TermName("add" + sing)}(${columnId(sing)} :
              ${TypeName("Long")})($session) =
                ${TermName(objectName(assocTableName(desc.name,
                sing)))}.insert(${TermName(assocTableName(desc.name,
                f.typeName))}(xid, ${columnId(f.typeName)}))
            """,
            q"""
            def ${TermName("remove" + f.name.capitalize)}(ids:
              Traversable[Long])($session) = {
              val assoc = for {
                x <- self.${
                  TermName(objectName(assocTableName(desc.name, f.typeName)))
                } if x.${columnId(desc.name)} === id &&
                x.${columnId(singL)}.inSet(ids)
              } yield x
              assoc.delete
            }
            """
          )
        } flatten
        val bases = Seq(
          (augment ? tq"Model"),
          (uuids ? tq"Uuids")
        ).flatten
        q"""
        case class ${TypeName(desc.name)}(..$idval, ..$valdefs,
          ..${desc.dateVals}, ..$uuidval)
        extends ..$bases
        {
          ..$xid
          ..$defdefs
          ..$one2manyDefs
          ..$one2manyDefAdds
        }
        """
      }
    }

    def mkColumn(desc: FldDesc): Tree = {
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

    /**
     * create the field1 ~ field2 ~ ... ~ fieldN string from case class column
     * does not handle correctly case classes with a single column (adding a dummy field would probably help)
     */
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

    /**
     * create the def * = ... from fields names and case class names
     */
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

    def mkForInsert(desc: ClsDesc): Tree = {
      val expr = {
        if (desc.timestamps)
          s"""def forInsert = (${mkTilde(desc.flat)}, dateCreated, lastUpdated).shaped <> ({
            case (${mkCase(desc.flat)}, dateCreated, lastUpdated) ⇒ ${desc.name}(None, ${mkCaseApply(desc.flat)}, dateCreated, lastUpdated)
          }, { x : ${desc.name} ⇒ Some((${mkCaseUnapply(desc.flat)}, x.dateCreated, x.lastUpdated))
          })"""
        else
          s"""def forInsert = (${mkTilde(desc.flat)}).shaped <> ({
            case (${mkCase(desc.flat)}) ⇒ ${desc.name}(None, ${mkCaseApply(desc.flat)})
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

    def mkTable(desc: ClsDesc, augment: Boolean = true)
    (implicit classes: List[ClsDesc]): List[Tree] = {
      if (desc.part)
        List(desc.tree.asInstanceOf[ClassDef])
      else {
        val listVals = desc.listValDefs
        val foreignKeys = desc.foreignKeys.map {
          it ⇒
            val cls = classes.find(it.typeName == _.name).getOrElse(throw new Exception(s"Invalid foreign class ${it.name}:${it.typeName}"))
            c.parse( s"""def ${it.name} = foreignKey("${it.name.toLowerCase}${desc.name}2${it.typeName.toLowerCase}", ${colIdName(it.name)}, ${objectName(it.typeName)})(_.id) """)
        }
        val assocs = desc.assocs.map { it ⇒
          val name = assocTableName(desc.name, it.typeName)
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
          if (v.part) v.cls.get.fields.map(mkColumn)
          else List(mkColumn(v))
        }
        val times = mkTimes(desc, augment)
        val forInsert = mkForInsert(desc)
        val vparams = q"""${TermName("tag")}:${TypeName("Tag")}""" :: Nil
        val plur = plural(decapitalize(desc.name)).toLowerCase
        val table = q"""
        class ${desc.tableType}(tag: Tag)
        extends Table[${TypeName(desc.name)}](tag, $plur)
        {
          ..$idCol
          ..$uuidCol
          ..${desc.dateDefs}
          ..$defdefs
          ..$foreignKeys
          $times
        }
        """
        model(desc, augment) :: table :: query(desc) :: assocTables
      }
    }

    def query(desc: ClsDesc)(implicit classes: List[ClsDesc]) = {
      val bases = listIf(!desc.assoc) {
        val crud = desc.timestamps ? tq"CrudEx" / tq"Crud"
        tq"$crud[${desc.typeName}, ${desc.tableType}]"
      }
      q"""
      val ${desc.obj} =
        new TableQuery(tag ⇒ new ${desc.tableType}(tag)) with ..$bases
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
      val assocs = cls.assocs map { a ⇒ (a.name, q"obj.${a.load}.list.uuids") }
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
          q"TableMetadata(${name}, ${TermName(name)})"
        }
      }
      q"""
      val tables = List(..$data)
      """
    }

    val (name, bases, body) = annottees.map(_.tree).toList match {
      case (o: ModuleDef) :: Nil ⇒
        o match {
          case q"object $name extends ..$bases { ..$body }" ⇒
            (name, bases, body)
          case _ ⇒
            c.abort(c.enclosingPosition, "Malformed DB definition")
        }
      case _ ⇒
        c.abort(c.enclosingPosition, "DB definition must be single object")
    }
    val parents = bases map { _.toString }
    val timestampsAll = parents.contains("Timestamps")
    val uuids = parents.contains("Uuids")
    val allDefs = defMap(body)
    implicit val classes = allDefs.getOrElse(CLASSDEF, Nil).map(it ⇒
        ClsDesc(it._2, timestampsAll, uuids))
    classes.foreach(_.parseBody(classes))
    val tables = classes.flatMap(mkTable(_))
    val enums = allDefs.getOrElse(ENUMDEF, Nil).map(_._2) flatMap(enum)
    val defdefs = allDefs.getOrElse(DEFDEF, Nil).map(_._2)
    val imports = allDefs.getOrElse(IMPORTDEF, Nil).map(_._2)
    val otherdefs = allDefs.getOrElse(OTHERDEF, Nil).map(_._2)
    val embeds = allDefs.getOrElse(EMBEDDEF, Nil).map(_._2)
    val jsonCodecs = if (uuids) classes map(jsonCodec) else Nil
    val metadata = createMetadata
    val result = q"""
    object $name extends ..$bases { self ⇒
      import scala.slick.util.TupleMethods._
      import scala.slick.jdbc.JdbcBackend
      import scala.slick.driver.SQLiteDriver.simple._
      import slickmacros.dao.Crud._
      import slickmacros.Uuids._
      import slickmacros.Implicits._
      import slickmacros._
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
      $metadata

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
