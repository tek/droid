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
    val INDEX = Value
    val PK = Value
    val UNIQUE = Value
    val DBTYPE = Value
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
    lazy val unique = flags.contains(FieldFlag.UNIQUE)

    lazy val part = flags.contains(FieldFlag.PART)

    lazy val option = flags.contains(FieldFlag.OPTION)

    lazy val cse = flags.contains(FieldFlag.CASE)

    lazy val pk = flags.contains(FieldFlag.PK)

    lazy val term = TermName(name)

    lazy val load = TermName(s"load${name.capitalize}")

    lazy val query = TermName(objectName(typeName))

    lazy val colId = columnId(name)

    lazy val sqlColId = sqlColName(colIdName(name))
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
            case Apply(Ident(constraintsTerm), List(Block(stats, expr))) ⇒
              Some(plural(decapitalize(clsName.toString)), stats :+ expr)
            case Apply(Apply(Ident(constraintsTerm), List(Literal(Constant(arg)))), List(Block(stats, expr))) ⇒
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
      val constraintsTerm = TermName("constraints")
      val ClassDef(mod, name, Nil, Template(parents, self, body)) = tree
      body.foreach {
        it ⇒
          it match {
            case ValDef(_, _, _, _) ⇒ fields += FldDesc(it, tree, allClasses)
            case _ ⇒
          }
      }
    }

    def assoc: Boolean = tree == null

    def part: Boolean = flags.contains(PARTDEF)

    def entity: Boolean = flags.contains(ENTITYDEF)

    def timestamps: Boolean = flags.contains(TIMESTAMPSDEF)

    def uuids: Boolean = flags.contains(UUIDSDEF)

    def dateVals = listIfList(timestamps) {
      List(
        q" var dateCreated: DateTime = null",
        q" var lastUpdated: DateTime = null"
        )
    }

    def dateDefs = listIfList(timestamps) {
      List(
        q""" def dateCreated = column[DateTime]("date_created") """,
        q""" def lastUpdated = column[DateTime]("last_updated") """
        )
      }

    def foreignKeys: List[FldDesc] = {
      fields.filter { it ⇒
        it.flags.contains(FieldFlag.CASE) &&
          !it.flags.contains(FieldFlag.LIST)
      } toList
    }

    def assocs: List[FldDesc] = {
      fields.filter { it ⇒
        it.flags.contains(FieldFlag.CASE) &&
          it.flags.contains(FieldFlag.LIST)
      } toList
    }

    def assocNames = {
      assocs map { ass ⇒ assocTableName(name, ass.typeName) }
    }

    def names = name :: assocNames

    def queries = names map { n ⇒ Helpers.plural(decapitalize(n)) }

    def tableName = mkTableName(name)

    def tableType = TypeName(mkTableName(name))

    def attrs = {
      fields.filter {
        it ⇒ !it.flags.contains(FieldFlag.CASE)
      } toList
    }

    def simpleValDefs: List[FldDesc] = {
      fields.filter {
        it ⇒ !it.flags.contains(FieldFlag.LIST)
      } toList
    }

    def listValDefs: List[FldDesc] = {
      fields.filter {
        it ⇒ it.flags.contains(FieldFlag.LIST)
      } toList
    }

    def listPKs: List[FldDesc] = {
      fields.filter {
        it ⇒ it.flags.contains(FieldFlag.PK)
      } toList
    }

    def allFields = {
      fields.toList.map {
        it ⇒
          if (it.part)
            it.cls.get.fields toList
          else
            it :: Nil
      } flatten
    }

    def indexes: List[FldDesc] = {
      allFields.filter {
        it ⇒
          it.flags.contains(FieldFlag.INDEX)
      } toList
    }
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

    def mkCaseClass(desc: ClsDesc, augment: Boolean = true)
    (implicit caseDefs: List[ClsDesc]): ClassDef = {
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
        val valdefs = desc.simpleValDefs.map {
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
        val one2manyDefs = desc.assocs.map {
          it ⇒
            q"""
            def ${TermName("load" + it.name.capitalize)} = for {
              x <- self.${TermName(objectName(assocTableName(desc.name,
              it.typeName)))} if x.${columnId(desc.name)} === id
              y <- self.${TermName(objectName(it.typeName))} if x.${columnId(it.typeName)} === y.id
          } yield(y)
            """
        }
        val one2manyDefAdds = desc.assocs.map { it ⇒
          val sing = it.typeName
          val singL = decapitalize(sing)
          Seq(
            q"""
            def ${TermName("add" + sing)}(${columnId(sing)} :
              ${TypeName("Long")})($session) =
                ${TermName(objectName(assocTableName(desc.name,
                sing)))}.insert(${TermName(assocTableName(desc.name,
                it.typeName))}(xid, ${columnId(it.typeName)}))
            """,
            q"""
            def ${TermName("remove" + it.name.capitalize)}(ids:
              Traversable[Long])($session) = {
              val assoc = for {
                x <- self.${
                  TermName(objectName(assocTableName(desc.name, it.typeName)))
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
    def mkTilde(fields: List[FldDesc]): String = {
      fields match {
        case Nil ⇒ c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case field :: Nil ⇒
          if (field.part)
            "(" + mkTilde(field.cls.get.fields toList) + ")"
          else if (field.cse)
            colIdName(field.name)
          else
            field.name
        case head :: tail ⇒ s"${mkTilde(head :: Nil)}, ${mkTilde(tail)}"
      }
    }

    def mkCaseApply(fields: List[FldDesc]): String = {
      fields match {
        case Nil ⇒ c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case field :: Nil ⇒
          if (field.part)
            s"${field.typeName}.tupled.apply(${field.name})"
          else if (field.cse)
            colIdName(field.name)
          else
            field.name
        case head :: tail ⇒ s"${mkCaseApply(head :: Nil)}, ${mkCaseApply(tail)}"
      }
    }

    def mkCaseUnapply(fields: List[FldDesc]): String = {
      fields match {
        case Nil ⇒ c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case field :: Nil ⇒
          if (field.part) {
            s"${field.typeName}.unapply(x.${field.name}).get"
          } else if (field.cse)
            "x." + colIdName(field.name)
          else
            "x." + field.name
        case head :: tail ⇒ s"${mkCaseUnapply(head :: Nil)}, ${mkCaseUnapply(tail)}"
      }
    }

    def mkCase(fields: List[FldDesc]): String = {
      fields match {
        case Nil ⇒ c.abort(c.enclosingPosition, "Cannot create table with zero column")
        case field :: Nil ⇒
          if (field.part) {
            field.name
          } else if (field.cse)
            colIdName(field.name)
          else
            field.name
        case head :: tail ⇒ s"${mkCase(head :: Nil)}, ${mkCase(tail)}"
      }
    }

    /**
     * create the def * = ... from fields names and case class names
     */
    def mkTimes(desc: ClsDesc, augment: Boolean = true): Tree = {
      val expr = {
        if (augment)
          if (desc.timestamps)
            s"""def * = (id.?, ${mkTilde(desc.simpleValDefs)}, dateCreated, lastUpdated).shaped <> ({
            case (id, ${mkCase(desc.simpleValDefs)}, dateCreated, lastUpdated) ⇒ ${desc.name}(id, ${mkCaseApply(desc.simpleValDefs)}, dateCreated, lastUpdated)
          }, { x : ${desc.name} ⇒ Some((x.id, ${mkCaseUnapply(desc.simpleValDefs)}, x.dateCreated, x.lastUpdated))
          })"""
          else
            s"""def * = (id.?, ${mkTilde(desc.simpleValDefs)}).shaped <> ({
            case (id, ${mkCase(desc.simpleValDefs)}) ⇒ ${desc.name}(id, ${mkCaseApply(desc.simpleValDefs)})
          }, { x : ${desc.name} ⇒ Some((x.id, ${mkCaseUnapply(desc.simpleValDefs)}))
          })"""
        else
        //s"""def * = (${mkTilde(desc.fields toList)}).shaped <> (${desc.name}.tupled, ${desc.name}.unapply _)"""
          s"""def * = (${mkTilde(desc.fields toList)}).shaped <> ({
            case (${mkCase(desc.simpleValDefs)}) ⇒ ${desc.name}( ${mkCaseApply(desc.simpleValDefs)})
          }, { x : ${desc.name} ⇒ Some((${mkCaseUnapply(desc.simpleValDefs)}))
          })"""

      }
      c.parse(expr)
    }

    def mkForInsert(desc: ClsDesc): Tree = {
      val expr = {
        if (desc.timestamps)
          s"""def forInsert = (${mkTilde(desc.simpleValDefs)}, dateCreated, lastUpdated).shaped <> ({
            case (${mkCase(desc.simpleValDefs)}, dateCreated, lastUpdated) ⇒ ${desc.name}(None, ${mkCaseApply(desc.simpleValDefs)}, dateCreated, lastUpdated)
          }, { x : ${desc.name} ⇒ Some((${mkCaseUnapply(desc.simpleValDefs)}, x.dateCreated, x.lastUpdated))
          })"""
        else
          s"""def forInsert = (${mkTilde(desc.simpleValDefs)}).shaped <> ({
            case (${mkCase(desc.simpleValDefs)}) ⇒ ${desc.name}(None, ${mkCaseApply(desc.simpleValDefs)})
          }, { x : ${desc.name} ⇒ Some((${mkCaseUnapply(desc.simpleValDefs)}))
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

    def cleanupBody(body: List[Tree]): List[Tree] = {
      val cleanDefs = List("embed", "onDelete", "index", "timestamps", "dbType")
      body filter {
        it ⇒
          it match {
            case Apply(Ident(func), List(Ident(field), Literal(dbType))) if cleanDefs.contains(func.toString) ⇒ false
            case _ ⇒ true
          }
      }
    }

    object ColInfo extends Enumeration {
      type ColInfo = Value
      val DBTYPE = Value
      val TIMESTAMPS = Value
      val INDEX = Value
      val ONDELETE = Value
      val PK = Value
    }
    import ColInfo._

    def caseInfo(body: List[Tree]): Map[ColInfo, List[(ColInfo, (String, Tree))]] = {
      body collect {
        case Apply(Ident(func), List(Ident(field), dbType)) if func.toString == "dbType" ⇒ (DBTYPE, (field.toString, dbType))
        case Apply(Ident(func), List(literal)) if func.toString == "timestamps" ⇒ (TIMESTAMPS, (null, literal))
        case Apply(Ident(func), List(Ident(field), isUnique)) if func.toString == "index" ⇒ (INDEX, (field.toString, isUnique))
        case Apply(Ident(func), List(Ident(field), action)) if func.toString == "onDelete" ⇒ (ONDELETE, (field.toString, action))
      } groupBy (_._1)
    }

    def mkTable(desc: ClsDesc, augment: Boolean = true)(implicit caseDefs: List[ClsDesc]): List[Tree] = {
      if (desc.part)
        List(desc.tree.asInstanceOf[ClassDef])
      else {
        val simpleVals = desc.simpleValDefs
        val listVals = desc.listValDefs
        val indexes = desc.indexes
        val foreignKeys = desc.foreignKeys.map {
          it ⇒
            val cls = caseDefs.find(it.typeName == _.name).getOrElse(throw new Exception(s"Invalid foreign class ${it.name}:${it.typeName}"))
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
        val assocTables = assocs.flatMap {
          it ⇒ mkTable(it, false)
        }
        val idCol = listIf(augment) {
          q"""
          def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
          """
        }
        val uuidCol = listIf(desc.uuids) {
          q""" def uuid = column[String]("uuid") """
        }
        val defdefs = simpleVals.flatMap {
          it ⇒
            if (it.part) {
              it.cls.get.fields.map {
                fld ⇒
                  mkColumn(fld)
              }
            } else {
              List(mkColumn(it))
            }
        }
        val indexdefs: List[c.universe.Tree] = indexes.map {
          it ⇒
            q"""def ${TermName(it.name + "Index")} = index(${"idx_" + desc.name.toLowerCase + "_" + it.name.toLowerCase}, ${TermName(it.name)}, ${it.unique})"""
        }
        val times = mkTimes(desc, augment)
        val forInsert = mkForInsert(desc)
        val vparams = q"""${TermName("tag")}:${TypeName("Tag")}""" :: Nil
        val plur = plural(decapitalize(desc.name)).toLowerCase
        val tableDef = q"""
        class ${desc.tableType}(tag: Tag)
        extends Table[${TypeName(desc.name)}](tag, $plur)
        {
          ..$idCol
          ..$uuidCol
          ..${desc.dateDefs}
          ..$defdefs
          ..$indexdefs
          ..$foreignKeys
          $times
        }
        """
        List(mkCaseClass(desc, augment), tableDef) ++ mkCompanion(desc) ++ assocTables
      }
    }

    def mkCompanion(desc: ClsDesc)(implicit caseDefs: List[ClsDesc]) = {
      val ex = if (desc.timestamps) "Ex" else ""
      val crud = if (!desc.assoc) s"with Crud$ex[${desc.name}, ${mkTableName(desc.name)}]" else ""

      val query = c.parse(s"""val ${TermName(objectName(desc.name))} =
        new TableQuery(tag ⇒ new ${desc.tableType}(tag)) $crud
      """)
      query :: Nil
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
    implicit val caseDefs = allDefs.getOrElse(CLASSDEF, Nil).map(it ⇒
        ClsDesc(it._2, timestampsAll, uuids))
    caseDefs.foreach(_.parseBody(caseDefs))
    val tables = caseDefs.flatMap(mkTable(_))
    val enums = allDefs.getOrElse(ENUMDEF, Nil).map(_._2) flatMap(enum)
    val defdefs = allDefs.getOrElse(DEFDEF, Nil).map(_._2)
    val imports = allDefs.getOrElse(IMPORTDEF, Nil).map(_._2)
    val otherdefs = allDefs.getOrElse(OTHERDEF, Nil).map(_._2)
    val embeds = allDefs.getOrElse(EMBEDDEF, Nil).map(_._2)
    val jsonCodecs = if (uuids) caseDefs map(jsonCodec) else Nil
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
