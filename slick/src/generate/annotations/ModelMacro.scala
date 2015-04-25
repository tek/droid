package slickmacros.annotations

import scala.reflect.macros.whitebox.Context
import scala.annotation.StaticAnnotation
import scala.language.existentials
import language.experimental.macros
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set
import scala.slick.model.ForeignKeyAction

class Slick(driver: String = "PostgresDriver", timestamps: Boolean = false)
extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro SlickMacro.impl
}

class Entity(name: String = null, timestamps: Boolean = false) extends StaticAnnotation

class Col(name: String = null, tpe: String = null, index: Boolean = false, unique: Boolean = false, pk: Boolean = false, onDelete: ForeignKeyAction = ForeignKeyAction.NoAction, onUpdate: ForeignKeyAction = ForeignKeyAction.NoAction, oldName: String = null) extends StaticAnnotation

trait Timestamps

trait Part

trait Uuids

object SlickMacro
{
  object FieldIndex extends Enumeration {
    type FieldIndex = Value
    val unique = Value(1)
    val indexed = Value(2)

  }

  import FieldIndex._

  implicit def anyToFieldOps(x: Any): FieldOps = null

  implicit def tuple2ToOps(x: (Any, Any)): FieldOps = null

  implicit def tuple3ToOps(x: (Any, Any, Any)): FieldOps = null

  implicit def tuple4ToOps(x: (Any, Any, Any, Any)): FieldOps = null

  implicit def tuple5ToOps(x: (Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple6ToOps(x: (Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple7ToOps(x: (Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple8ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple9ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple10ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple11ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple12ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple13ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple14ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple15ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple16ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple17ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple18ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple19ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple20ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple21ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  implicit def tuple22ToOps(x: (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any)): FieldOps = null

  trait FieldOps {
    def is(x: FieldIndex): FieldOps = null

    def renamed(x: Any): FieldOps = null

    def are(x: FieldIndex): FieldOps = null

    def withType(x: String): FieldOps = null

    def withName(x: String): FieldOps = null

    def onUpdate(x: ForeignKeyAction): FieldOps = null

    def onDelete(x: ForeignKeyAction): FieldOps = null

    def to(x: Any): FieldOps = null
  }

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
  import FieldIndex._
  import DefType._
  import ClassFlag._
  import FieldFlag._

  def listIf(indicator: Boolean)(ctor: ⇒ Tree): List[Tree] = {
    if (indicator) List(ctor)
    else List[Tree]()
  }

  val session = q"implicit val session: JdbcBackend#SessionDef"

  def constraints(plural: String = null)(f: ⇒ Unit) {}

  def impl(annottees: c.Expr[Any]*) = {
    import Flag._

    lazy val dateTime = Seq(q"""
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

  def plural(name: String) = {
    val rules = List(
      ("(\\w*)people$", "$1people"),
      ("(\\w*)children$", "$1children"),
      ("(\\w*)feet$", "$1feet"),
      ("(\\w*)teeth$", "$1teeth"),
      ("(\\w*)men$", "$1men"),
      ("(\\w*)equipment$", "$1equipment"),
      ("(\\w*)information$", "$1information"),
      ("(\\w*)rice$", "$1rice"),
      ("(\\w*)money$", "$1money"),
      ("(\\w*)fish$", "$fish"),
      ("(\\w*)sheep$", "$1sheep"),
      ("(\\w+)(es)$", "$1es"),
      // Check exception special case words
      ("(\\w*)person$", "$1people"),
      ("(\\w*)child$", "$1children"),
      ("(\\w*)foot$", "$1feet"),
      ("(\\w*)tooth$", "$1teeth"),
      ("(\\w*)bus$", "$1buses"),
      ("(\\w*)man$", "$1men"),
      ("(\\w*)(ox|oxen)$", "$1$2"),
      ("(\\w*)(buffal|tomat)o$", "$1$2oes"),
      ("(\\w*)quiz$", "$1$2zes"),
      // Greek endings
      ("(\\w+)(matr|vert|ind)ix|ex$", "$1$2ices"),
      ("(\\w+)(sis)$", "$1ses"),
      ("(\\w+)(um)$", "$1a"),
      // Old English. hoof -> hooves, leaf -> leaves
      ("(\\w*)(fe)$", "$1ves"),
      ("(\\w*)(f)$", "$1ves"),
      ("(\\w*)([m|l])ouse$", "$1$2ice"),
      // Y preceded by a consonant changes to ies
      ("(\\w+)([^aeiou]|qu)y$", "$1$2ies"),
      // Voiced consonants add es instead of s
      ("(\\w+)(z|ch|sh|as|ss|us|x)$", "$1$2es"),
      // Check exception special case words
      ("(\\w*)cactus$", "$1cacti"),
      ("(\\w*)focus$", "$1foci"),
      ("(\\w*)fungus$", "$1fungi"),
      ("(\\w*)octopus$", "$1octopi"),
      ("(\\w*)radius$", "$1radii"),
      // If nothing else matches, and word ends in s, assume plural already
      ("(\\w+)(s)$", "$1s"))
    rules.find(it ⇒ name.matches(it._1)).map(it ⇒ name.replaceFirst(it._1, it._2)).getOrElse(name.replaceFirst("([\\w]+)([^s])$", "$1$2s"))
  }

    val reservedNames = List("id", "dateCreated", "lastUpdated")
    val caseAccessor = scala.reflect.internal.Flags.CASEACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramAccessor = scala.reflect.internal.Flags.PARAMACCESSOR.asInstanceOf[Long].asInstanceOf[FlagSet]
    val prvate = scala.reflect.internal.Flags.PRIVATE.asInstanceOf[Long].asInstanceOf[FlagSet]
    val local = scala.reflect.internal.Flags.LOCAL.asInstanceOf[Long].asInstanceOf[FlagSet]
    val paramDefault = scala.reflect.internal.Flags.DEFAULTPARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val param = scala.reflect.internal.Flags.PARAM.asInstanceOf[Long].asInstanceOf[FlagSet]
    val mutable = scala.reflect.internal.Flags.MUTABLE.asInstanceOf[Long].asInstanceOf[FlagSet]
    val optionalDate = Select(Select(Select(Ident(TermName("org")), TermName("joda")), TermName("time")), TypeName("DateTime"))
    val caseparam = Modifiers(caseAccessor | paramAccessor)
    val paramparam = Modifiers(param | paramAccessor)
    def idVal(tpeName: TypeName) = q"$caseparam val id:Option[$tpeName]"
    def idValInCtor(tpeName: TypeName) = q"$paramparam val id:Option[$tpeName]"
    def dateVal(name: String) = ValDef(Modifiers(mutable | caseAccessor | paramAccessor | paramDefault), TermName(name), optionalDate, Literal(Constant(null)))

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

      def part: Boolean = flags.exists(_ == PARTDEF)

      def entity: Boolean = flags.exists(_ == ENTITYDEF)

      def timestamps: Boolean = flags.exists(_ == TIMESTAMPSDEF)

      def uuids: Boolean = flags.exists(_ == UUIDSDEF)

      def dateVals: List[ValDef] = if (timestamps) dateVal("dateCreated") :: dateVal("lastUpdated") :: Nil else Nil

      def dateDefs =
        if (timestamps)
          c.parse( """def dateCreated = column[org.joda.time.DateTime]("date_created")""") :: c.parse( """def lastUpdated = column[org.joda.time.DateTime]("last_updated")""") :: Nil
        else
          Nil

      def foreignKeys: List[FldDesc] = {
        fields.filter {
          it ⇒ it.flags.exists(_ == FieldFlag.CASE) && !it.flags.exists(_ == FieldFlag.LIST)
        } toList
      }

      def assocs: List[FldDesc] = {
        fields.filter {
          it ⇒ it.flags.exists(_ == FieldFlag.CASE) && it.flags.exists(_ == FieldFlag.LIST)
        } toList
      }

      def attrs = {
        fields.filter {
          it ⇒ !it.flags.contains(FieldFlag.CASE)
        } toList
      }

      def simpleValDefs: List[FldDesc] = {
        fields.filter {
          it ⇒ !it.flags.exists(_ == FieldFlag.LIST)
        } toList
      }

      def listValDefs: List[FldDesc] = {
        fields.filter {
          it ⇒ it.flags.exists(_ == FieldFlag.LIST)
        } toList
      }

      def listPKs: List[FldDesc] = {
        fields.filter {
          it ⇒ it.flags.exists(_ == FieldFlag.PK)
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
            it.flags.exists(_ == FieldFlag.INDEX)
        } toList
      }
    }

    object ClsDesc {
      def apply(tree: Tree, timestampAll: Boolean, uuids: Boolean) = {
        val ClassDef(mod, name, Nil, Template(parents, _, body)) = tree
        if (!mod.hasFlag(CASE))
          c.abort(c.enclosingPosition, s"Only case classes allowed here ${name.decodedName.toString}")

        val annotations = mod.annotations.map(_.children.head.toString)
        val isPart = annotations.exists(_ == "new Part") || parents.exists(_.toString.contains("Part"))
        val flags = Set[ClassFlag]()
        if (isPart)
          flags += PARTDEF
        else
          flags += ENTITYDEF
        val timestamps = parents.exists(_.toString.contains("Timestamps"))
        if (timestampAll || timestamps) flags += TIMESTAMPSDEF
        if (uuids) flags += UUIDSDEF
        new ClsDesc(name.decodedName.toString, flags, ListBuffer(), tree, plural(decapitalize(name.decodedName.toString)))
      }
    }

  case class FldDesc(name: String, colName: String, typeName: String, flags:
    Set[FieldFlag], dbType: Option[String], onDelete: String, onUpdate: String,
    cls: Option[ClsDesc], tree: Tree) {
      def unique: Boolean = flags.exists(_ == FieldFlag.UNIQUE)

      def part: Boolean = flags.exists(_ == FieldFlag.PART)

      def option: Boolean = flags.exists(_ == FieldFlag.OPTION)

      def cse: Boolean = flags.exists(_ == FieldFlag.CASE)

      def pk: Boolean = flags.exists(_ == FieldFlag.PK)

      def term = TermName(name)

      def load = TermName(s"load${name.capitalize}")

      def onDeleteAction = s"scala.slick.model.ForeignKeyAction.$onDelete"

      def onUpdateAction = s"scala.slick.model.ForeignKeyAction.$onUpdate"
    }

    case class ScalaAnnotation(val name: String, val fields: Array[String]) {
      def field(i: Int): String = {
        if (fields.size <= i)
          c.abort(c.enclosingPosition, s"field at position $i is required in annotation $name")
        val res = fields(i)
        if (res.startsWith( """"""")) res.substring(1, res.length - 1) else res
      }
    }

    object ScalaAnnotation {
      def apply(expr: Tree) = {
        val sexpr = expr.toString
        if (!sexpr.startsWith("new "))
          c.abort(c.enclosingPosition, s"Invalid annotation $sexpr")
        else {
          val name = sexpr.substring("new ".length, sexpr.indexOf('('))
          val params = sexpr.substring(sexpr.indexOf('(') + 1, sexpr.lastIndexOf(')')).split(',').map {
            case it if (it.contains("=")) ⇒
              c.abort(c.enclosingPosition, s"Named parameters not uspported on  Slick-macros Annotations")
            case other ⇒
              other.toString
          }
          new ScalaAnnotation(name, params)
        }
      }
    }

    object FldDesc {
      def apply(fieldTree: Tree, clsTree: Tree, allClasses: List[ClsDesc]) = {
        val ValDef(mod, name, tpt, rhs) = fieldTree
        if (reservedNames.contains(name.decodedName.toString))
          c.abort(c.enclosingPosition,
            s"Column with name ${name.decodedName.toString} not allowed")
        val flags = Set[FieldFlag]()
        val annotation = mod.annotations.headOption.map(_.children.head.toString)
        var colType: String = null
        var colName: String = asColName(name.decodedName.toString)
        var onDelete: String = "NoAction"
        var onUpdate: String = "NoAction"

        mod.annotations.map(x ⇒ x.toString).foreach {
          annotation ⇒
            val params = annotation.substring(annotation.indexOf('(') + 1, annotation.lastIndexOf(')')).split(',').map(_.split("="))
            if (annotation startsWith "new Col(") {
              val paramsMap = Map((0 -> "name"), (1 -> "tpe"), (2 -> "index"), (3 -> "unique"), (4 -> "pk"), (4 -> "onDelete"), (5 -> "onUpdate"))
              val named = params.view.zipWithIndex.map {
                case (param, i) ⇒
                  if (param.length == 1)
                    (paramsMap(i), param(0).trim)
                  else {
                    (param(0).trim, param(1).trim)
                  }
              }
              named.foreach {
                param ⇒

                  param._1 match {
                    case "onDelete" ⇒ onDelete = param._2.replaceAll("^.*\\.", "")
                    case "onUpdate" ⇒ onUpdate = param._2.replaceAll("^.*\\.", "")
                    case "name" ⇒ colName = param._2.replaceAll( """^"|"$""", "")
                    case "tpe" ⇒
                      flags += FieldFlag.DBTYPE
                      colType = param._2.replaceAll( """^"|"$""", "")
                    case "index" if param._2.contains("true") ⇒ flags += FieldFlag.INDEX
                    case "unique" if param._2.contains("true") ⇒
                      flags += FieldFlag.INDEX
                      flags += FieldFlag.UNIQUE
                    case "pk" if param._2.contains("true") ⇒
                      flags += FieldFlag.PK
                  }
              }
            } else
              c.abort(c.enclosingPosition, s"Invalid $annotation on column ${name.decodedName.toString}")
        }
        def buildTypeName(tree: Tree): String = {
          tree match {
            case Select(subtree, name) ⇒
              buildTypeName(subtree) + "." + name.decodedName.toString
            case AppliedTypeTree(subtree, args) ⇒
              buildTypeName(subtree) + "[" + args.map(it ⇒ buildTypeName(it)).mkString(",") + "]"
            case Ident(x) ⇒
              x.decodedName.toString
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
          case AppliedTypeTree(Ident(option), tpe :: Nil) if option.decodedName.toString == "Option" ⇒
            typeName = buildTypeName(tpe)
            flags += FieldFlag.OPTION
            val clsDesc = allClasses.find(_.name == typeName)
            clsDesc.foreach {
              it ⇒
                if (it.entity)
                  flags += FieldFlag.CASE
            }
            clsDesc
          case AppliedTypeTree(Ident(list), tpe :: Nil) if list.decodedName.toString == "List" ⇒
            typeName = buildTypeName(tpe)
            val clsDesc = allClasses.find(_.name == typeName).getOrElse(c.abort(c.enclosingPosition, s"List not allowed here ${name.decodedName.toString} not allowed"))

            if (clsDesc.entity)
              flags ++= Set(FieldFlag.CASE, FieldFlag.LIST)
            else
              c.abort(c.enclosingPosition, s"only entity allowed here ${name.decodedName.toString}:${clsDesc.name}")
            Some(clsDesc)
          case _ ⇒ None
        }
        val tree = mod.annotations
        tree.foreach {
          case Apply(Select(New(Ident(index)), _), List(Literal(Constant(unique)))) ⇒
            if (index.decodedName.toString == "Index") {
              flags += FieldFlag.INDEX
              if (unique == true) flags += FieldFlag.UNIQUE
            }

          case Apply(Select(New(Ident(pk)), _), _) ⇒
            if (pk.decodedName.toString == "PK") {
              flags += FieldFlag.PK
            }

          case Apply(Select(New(Ident(dbType)), _), List(Literal(Constant(dbTypeValue)))) ⇒
            if (dbType.decodedName.toString == "Type") {
              flags += FieldFlag.DBTYPE
              colType = dbTypeValue.asInstanceOf[String]
            }
        }

        val ClassDef(_, clsName, _, Template(_, _, body)) = clsTree
        body.foreach {
          it ⇒
            val cns = it match {
              case Apply(Ident(constraintsTerm), List(Block(stats, expr))) ⇒
                Some(plural(decapitalize(clsName.decodedName.toString)), stats :+ expr)
              case Apply(Apply(Ident(constraintsTerm), List(Literal(Constant(arg)))), List(Block(stats, expr))) ⇒
                Some(arg.toString, stats :+ expr)
              case _ ⇒ None
            }
            cns foreach {
              it ⇒
                allClasses.find(_.name == clsName.decodedName.toString).foreach {
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
                      if (fieldNames.contains(name.decodedName.toString)) {
                        st.drop(1).foreach {
                          s ⇒
                            val method = s.substring(0, s.indexOf('(')).trim
                            val arg = s.substring(s.indexOf('(') + 1, s.lastIndexOf(')'))
                            method match {
                              case "is" | "are" ⇒
                                flags += FieldFlag.INDEX
                                if (arg == "unique") flags += FieldFlag.UNIQUE
                              case "withName" ⇒
                                colName = arg.substring(1, arg.length - 1)
                              case "withType" ⇒
                                flags += FieldFlag.DBTYPE; colType = arg.substring(1, arg.length - 1)
                              case "onUpdate" ⇒ onUpdate = arg
                              case "onDelete" ⇒ onDelete = arg
                            }
                        }
                      }
                    }
                }
            }
        }
        new FldDesc(name.decodedName.toString, colName, typeName, flags, Option(colType), onDelete, onUpdate, clsDesc, fieldTree)
      }
    }

    def mkCaseClass(desc: ClsDesc, augment: Boolean = true)
    (implicit caseDefs: List[ClsDesc]): ClassDef = {
      if (desc.part) {
        desc.tree.asInstanceOf[ClassDef]
      } else {
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
              val termName = TermName(nme.decodedName.toString + "Id")
              q"val $termName:$tpt"
            } else
              it.tree.asInstanceOf[ValDef]
        }
        val defdefs = desc.foreignKeys.map {
          it ⇒
            if (it.option)
              q"""def ${TermName("load" + it.name.capitalize)}(implicit session : JdbcBackend#SessionDef) = ${TermName(objectName(it.typeName))}.filter(_.id === ${TermName(colIdName(it.name))}).firstOption"""
            else
              q"""def ${TermName("load" + it.name.capitalize)}(implicit session : JdbcBackend#SessionDef) = ${TermName(objectName(it.typeName))}.filter(_.id === ${TermName(colIdName(it.name))}).first"""
        }
        val one2manyDefs = desc.assocs.map {
          it ⇒
            q"""
            def ${TermName("load" + it.name.capitalize)} = for {
              x <- self.${TermName(objectName(assocTableName(desc.name, it.typeName)))} if x.${TermName(colIdName(desc.name))} === id
              y <- self.${TermName(objectName(it.typeName))} if x.${TermName(colIdName(it.typeName))} === y.id
          } yield(y)
            """
        }
        val one2manyDefAdds = desc.assocs.map { it ⇒
          val sing = it.typeName
          val singL = decapitalize(sing)
          Seq(
            q"""
            def ${TermName("add" + sing)}(${TermName(colIdName(sing))} :
              ${TypeName("Long")})(implicit session : JdbcBackend#SessionDef) =
                ${TermName(objectName(assocTableName(desc.name, sing)))}.insert(${TermName(assocTableName(desc.name, it.typeName))}(xid, ${TermName(colIdName(it.typeName))}))
            """,
            q"""
            def ${TermName("remove" + it.name.capitalize)}(ids: Traversable[Long])(implicit session : JdbcBackend#SessionDef) = {
              val assoc = for {
                x <- self.${
                  TermName(objectName(assocTableName(desc.name, it.typeName)))
                } if x.${TermName(colIdName(desc.name))} === id &&
                x.${TermName(colIdName(singL))}.inSet(ids)
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

    def asColName(name: String): String = {
      name.toCharArray().zipWithIndex map {
        case (ch, i) if Character.isUpperCase(ch) && i > 0 ⇒
          "_" + Character.toLowerCase(ch)
        case (ch, _) ⇒ Character.toLowerCase(ch)
      } mkString
    }

    def mkColumn(desc: FldDesc): Tree = {
      val q"$mods val $nme:$tpt = $initial" = desc.tree
      if (desc.cse) {
        if (desc.option) {
          q"""def ${TermName(colIdName(desc.name))} = column[Option[Long]](${asColName(colIdName(desc.name))})"""
        } else {
          q"""def ${TermName(colIdName(desc.name))} = column[Long](${asColName(colIdName(desc.name))})"""
        }
      } else {
        val tpe = desc.typeName
        desc.dbType map {
          it ⇒
            q"""def $nme = column[$tpt](${desc.colName}, O.DBType(${it}))"""
        } getOrElse {
          q"""def $nme = column[$tpt](${desc.colName})"""
        }
      }
    }

    def colIdName(caseClassName: String) = {
      s"${decapitalize(caseClassName)}Id"
    }

    def tableName(typeName: String) = s"${typeName}Table"

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

    def objectName(typeName: String)(implicit caseDefs: List[ClsDesc]) = {
      caseDefs find { typeName == _.name } map { _.plural } getOrElse {
        plural(decapitalize(typeName))
      }
    }

    def assocTableName(table1: String, table2: String) = s"${table1}2${table2}"

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
          s"""implicit val ${name.decodedName.toString}TypeMapper = MappedColumnType.base[${name.decodedName.toString}.Value, $valueType](
            {
              it ⇒ it.id
            },
            {
              id ⇒ ${name}(id)
            })"""
        case "String" ⇒
          s"""implicit val ${name.decodedName.toString}TypeMapper = MappedColumnType.base[${name.decodedName.toString}.Value, $valueType](
            {
              it ⇒ it.toString
            },
            {
              id ⇒ ${name.decodedName.toString}.withName(id)
            })"""

      }
      c.parse(mapper)
    }

    def cleanupBody(body: List[Tree]): List[Tree] = {
      val cleanDefs = List("embed", "onDelete", "index", "timestamps", "dbType")
      body filter {
        it ⇒
          it match {
            case Apply(Ident(func), List(Ident(field), Literal(dbType))) if cleanDefs.contains(func.decodedName.toString) ⇒ false
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
        case Apply(Ident(func), List(Ident(field), dbType)) if func.decodedName.toString == "dbType" ⇒ (DBTYPE, (field.decodedName.toString, dbType))
        case Apply(Ident(func), List(literal)) if func.decodedName.toString == "timestamps" ⇒ (TIMESTAMPS, (null, literal))
        case Apply(Ident(func), List(Ident(field), isUnique)) if func.decodedName.toString == "index" ⇒ (INDEX, (field.decodedName.toString, isUnique))
        case Apply(Ident(func), List(Ident(field), action)) if func.decodedName.toString == "onDelete" ⇒ (ONDELETE, (field.decodedName.toString, action))
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
            //val fkAction = if (colInfo.isDefined && colInfo.get.onDelete != null) colInfo.get.onDelete else "ForeignKeyAction.NoAction"
            c.parse( s"""def ${it.name} = foreignKey("${it.name.toLowerCase}${desc.name}2${it.typeName.toLowerCase}", ${colIdName(it.name)}, ${objectName(it.typeName)})(_.id, ${it.onUpdateAction}, ${it.onDeleteAction}) """) // onDelete
        }
        val assocs = desc.assocs.map { it ⇒
          new ClsDesc(assocTableName(desc.name, it.typeName), Set(ENTITYDEF),
            ListBuffer(
              new FldDesc(decapitalize(desc.name), decapitalize(desc.name),
                desc.name, Set(FieldFlag.CASE), None, "NoAction", "NoAction",
                Some(desc), ValDef(caseparam, TermName(decapitalize(desc.name)), null, null)),
              new FldDesc(decapitalize(it.typeName),
                decapitalize(it.typeName), it.typeName, Set(FieldFlag.CASE),
                None, "NoAction", "NoAction", it.cls, ValDef(caseparam,
                  TermName(decapitalize(it.typeName)), null, null))),
            null, plural(decapitalize(assocTableName(desc.name, it.typeName))))

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
        val tagDef = ValDef(Modifiers(prvate | local | paramAccessor), TermName("tag"), Ident(TypeName("Tag")), EmptyTree)

        val vparams = q"""${TermName("tag")}:${TypeName("Tag")}""" :: Nil
        val plur = plural(decapitalize(desc.name)).toLowerCase
        val tableDef = q"""
        class ${TypeName(tableName(desc.name))}(tag: Tag)
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
      val crud = if (!desc.assoc) s"with Crud$ex[${desc.name}, ${tableName(desc.name)}]" else ""

      val query = c.parse(s"val ${TermName(objectName(desc.name))} = new TableQuery(tag ⇒ new ${TypeName(tableName(desc.name))}(tag)) $crud")
      query :: Nil
    }

    def defMap(body: List[c.universe.Tree]): Map[DefType, List[(DefType, c.universe.Tree)]] = {
      body.flatMap {
        it ⇒
          it match {
            case ModuleDef(mod, name, Template(List(Ident(t)), _, _)) if t.isTypeName && t.decodedName.toString == "Enumeration" ⇒ List((ENUMDEF, it))
            case ClassDef(mod, name, Nil, body) if mod.hasFlag(CASE) ⇒ List((CLASSDEF, it))
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
      val ident = TermName(s"codec for enum `${name}`")
      val json = q"""
      implicit val $ident = jencode1L { v: $name.$tpe ⇒ v.toString } ($nameS)
      """
      val mapper = mkEnumMapper(tree)
      List(tree, mapper, json)
    }

    def jsonCodec(cls: ClsDesc) = {
      val name = TypeName(cls.name)
      val ident = TermName(s"`${cls.name} json codec`")
      val encoder = TermName(s"jencode${cls.fields.length}L")
      val fks = cls.foreignKeys map { a ⇒ (a.name, q"obj.${a.load}.uuid") }
      val assocs = cls.assocs map { a ⇒ (a.name, q"obj.${a.load}.list.uuids") }
      val attrs = cls.attrs map { a ⇒ (a.name, q"obj.${a.term}") }
      val (names, values) = (fks ++ assocs ++ attrs).unzip
      q"""
      implicit def $ident (implicit session: JdbcBackend#SessionDef) =
        $encoder { obj: $name ⇒
          (..$values) }(..$names)
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
    val ann = ScalaAnnotation(c.prefix.tree)
    val driverName = ann.field(0)
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
