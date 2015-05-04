package slick

import scala.reflect.macros.whitebox.Context
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

trait SchemaBase
{
  def tableMap: Map[String, db.TableMetadata]
  def metadata: db.SchemaMetadata
}

object SchemaHelpers
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

trait SchemaMacrosBase
{
  val c: Context

  import c.universe._
  import SchemaHelpers._
  import DefType._
  import ClassFlag._
  import FieldFlag._
  import Helpers._
  import Flag._

  def create(annottees: c.Expr[Any]*) = {
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
    impl(name, bases, body)
  }

  def impl(name: TermName, bases: List[Tree], body: List[Tree]): c.Expr[Any]

  def listIf(indicator: Boolean)(ctor: ⇒ Tree): List[Tree] = {
    if (indicator) List(ctor)
    else List[Tree]()
  }

  def listIfList(indicator: Boolean)(ctor: ⇒ List[Tree]): List[Tree] = {
    if (indicator) ctor
    else List[Tree]()
  }

  val session = q"implicit val session: JdbcBackend#SessionDef"

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

  def decapitalize(name: String) = {
    if (name.length == 0) name
    else {
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

  def assocTableName(from: ClsDesc, to: FldDesc) =
    s"${from.name}2${to.singularName.capitalize}"

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

    lazy val loadMany = TermName(s"load${plural(name.capitalize)}")

    lazy val query = TermName(objectName(typeName))

    lazy val colId = columnId(name)

    lazy val sqlColId = sqlColName(colIdName(name))

    lazy val queryColId = columnId(typeName)

    lazy val singularName = singular(name)
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

    lazy val assocQuerys = assocs map { ass ⇒ assocName(ass) }

    lazy val names = name :: assocQuerys

    lazy val queries = names map(objectName)

    lazy val tableName = mkTableName(name)

    lazy val tableType = TypeName(mkTableName(name))

    lazy val query = TermName(objectName(name))

    lazy val typeName = TypeName(name)

    def assocQuery(other: FldDesc) =
      TermName(objectName(assocName(other)))

    def assocModel(other: FldDesc) = TermName(assocName(other))

    def assocName(other: FldDesc) = assocTableName(this, other)

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
}
