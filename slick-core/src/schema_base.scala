package slick

import scala.reflect.macros.whitebox.Context
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Set

import tryp.core.Annotation

trait SchemaBase
{
  def tableMap: Map[String, db.TableMetadata]
  def metadata: db.SchemaMetadata
}

trait SchemaMacrosBase
extends Annotation
{
  import c.universe._

  val session = q"implicit val s: Session"

  def dateTime = {
      Seq(
      q"""
      implicit val DateTimeTypeMapper =
        MappedColumnType.base[DateTime, Timestamp](
        { dt ⇒ new Timestamp(dt.getMillis) },
        { ts ⇒ new DateTime(ts.getTime) }
      )
      """
    )
  }

  val reservedNames = List("id", "created", "updated", "uuid")

  def sqlColName(name: String): String = {
    name.toCharArray().zipWithIndex map {
      case (ch, i) if Character.isUpperCase(ch) && i > 0 ⇒
        "_" + Character.toLowerCase(ch)
      case (ch, _) ⇒ Character.toLowerCase(ch)
    } mkString
  }

  implicit class SlickStringOps(s: String) {
    def plural = Helpers.plural(s)
    def decapitalize = Helpers.decapitalize(s)
    def singular = Helpers.singular(s)
    def objectName = TermName(s.decapitalize.plural)
    def u = s.capitalize
    def d = s.decapitalize
    def dp = s.d.plural
    def up = s.u.plural
    def ds = s.d.singular
    def us = s.u.singular
    def columnId = TermName(s.colIdName)
    def colIdName = s"${s.decapitalize}Id"
  }

  implicit def `TermName to SlickStringOps`(name: TermName) =
    new SlickStringOps(name.toString)

  implicit def `TypeName to SlickStringOps`(name: TypeName) =
    new SlickStringOps(name.toString)

  implicit def `Tree to SlickStringOps`(name: Tree) =
    new SlickStringOps(name.toString)

  implicit class `TermName extensions`(name: TermName) {
    def prefix(s: String) = TermName(s"${s}${name.u}")
    def prefixPlural(s: String) = TermName(s"${s}${name.up}")
    def capitalize = name.toString.capitalize
    def snakeCase = name.toString.snakeCase
  }

  implicit class `TypeName extensions`(name: TypeName) {
    def suffix(s: String) = TypeName(s"${name.u}${s}")
    def tree = {
      val pq"_: $t" = pq"_: $name"
      t
    }
  }

  implicit class `Tree extensions`(tree: Tree) {
    def typeName = {
      tree match {
        case tq"$a[..$b]" ⇒ TypeName(tree.toString)
        case pq"_: ${tn: TypeName}" ⇒ tn
        case tq"${tn: TypeName}" ⇒ tn
      }
    }
  }

  case class EnumSpec(name: TermName, body: List[Tree])

  trait EnumProcessor[A <: SchemaMacrosBase]
  {
    def apply(enum: EnumSpec): List[Tree]
  }

  trait BasicEnumProcessor[A <: SchemaMacrosBase]
  extends EnumProcessor[A]
  {
    def apply(enum: EnumSpec): List[Tree] = List(
      mapper(enum),
      q"import ${enum.name}._"
    )

    def mapper(enum: EnumSpec): Tree = {
      q"""
      object ${enum.name}
      extends Enumeration
      {
        ..${enum.body}
        implicit val mapper =
          MappedColumnType.base[Value, String](_.toString,
            ${enum.name}.withName)
      }
      """
    }
  }

  trait AttrSpecBase
  {
    def name: TermName
    def tpt: Tree

    def nameS = name.toString

    lazy val option = tpt match {
      case tq"Option[..$_]" ⇒ true
      case _ ⇒ false
    }

    lazy val actualType = AttrSpec.actualType(tpt)

    lazy val term = name

    lazy val load = name.prefix("load")

    lazy val loadMany = name.prefixPlural("load")

    lazy val replaceMany = name.prefixPlural("replace")

    def sqlColId = name.snakeCase

    lazy val assocQueryColId = name.ds.columnId

    lazy val singularName = name.singular

    lazy val valDef = q"val $name: $tpt = $default"

    lazy val paramName = name

    def column =
      q"def $colName = column[$colType]($sqlColId, ..$columnFlags)"

    def default: Tree = option ? q"None" / q""

    lazy val colName = name

    lazy val colId = name.columnId

    def colType = tpt

    def columnFlags: List[Select] = Nil

    def singularTerm = TermName(name.ds)

    def tilde: Tree = q"$colName"
  }

  case class AttrSpec(name: TermName, tpt: Tree, customDefault: Tree)
  extends AttrSpecBase
  {
    override def default = {
      if (customDefault != q"") customDefault
      else super.default
    }
  }

  trait ReferenceSpec
  {
    self: AttrSpecBase ⇒

    def query = TermName(actualType.toString)
  }

  case class ForeignKeySpec(name: TermName, tpt: Tree)
  extends AttrSpecBase
  with ReferenceSpec
  {
    override def colType = option ? tq"Option[$keyType]" / keyType

    def keyType = tq"Long"

    override lazy val colName = colId

    override def sqlColId = name.colIdName.snakeCase

    override lazy val valDef = q"val $colId: $colType"

    override lazy val paramName = colName

    def sqlFk = name.snakeCase

    def fkDef = q"def $name = foreignKey($sqlFk, $colId, $query)(_.id)"
  }

  case class AssociationSpec(name: TermName, tpt: Tree,
    override val default: Tree)
  extends AttrSpecBase
  with ReferenceSpec
  {
    override def column =
      throw new Exception("AssociationSpec can't become column")
  }

  case class IdColSpec()
  extends AttrSpecBase
  {
    def name = TermName("id")

    def tpt = tq"Long"

    override def default = q"0"

    override def columnFlags = List(q"O.PrimaryKey", q"O.AutoInc")
  }

  case class DateColSpec(desc: String)
  extends AttrSpecBase
  {
    def name = TermName(desc)

    def tpt = tq"DateTime"

    override def default = q"DateTime.now"
  }

  object AttrSpec {

    def actualType[A >: Tree with TypeName](input: A): Tree = {
      val tpt = input match {
        case n: TypeName ⇒ n.tree
        case t: Tree ⇒ t
      }
      tpt match {
        case tq"Option[..$act]" ⇒ act.head
        case tq"List[..$act]" ⇒ act.head
        case tq"${act}" ⇒ act
      }
    }

    def isAssoc(tpe: Tree) = {
      tpe match {
        case tq"List[..$act]" ⇒ true
        case _ ⇒ false
      }
    }
  }

  class TableOpsBase(m: TableSpec)
  {
    def name = m.name
    def params = m.params
    def bases = m.bases
    implicit def info = m.info
    def crudBase = tq"slick.db.CrudCompat"
  }

  abstract class TableOps[B <: TableSpec](m: TableSpec)
  extends TableOpsBase(m)
  {
    lazy val timestamps = info.timestamps

    def nameS = name.toString

    def pluralS = nameS.plural

    def path = name.dp.snakeCase

    lazy val assocQuerys = assocs map { ass ⇒ assocName(ass) }

    lazy val names = name.toString :: assocQuerys

    lazy val queries = names map(_.objectName)

    lazy val tableName = name.suffix("Table")

    lazy val query = term

    lazy val term = name.toTermName

    def assocQuery(other: AttrSpecBase) =
      TermName(assocName(other))

    def assocModel(other: AttrSpecBase) = TermName(assocName(other))

    def assocName(other: AttrSpecBase) = assocTableName(other)

    def assocTableName(to: AttrSpecBase) =
      s"${name}2${to.singularName.u}"

    def fieldType[A <: AttrSpecBase: ClassTag] = params.collect {
      case f: A ⇒ f
    }

    lazy val foreignKeys = fieldType[ForeignKeySpec]

    lazy val assocs = fieldType[AssociationSpec]

    lazy val attrs = fieldType[AttrSpec]

    def modelParams: List[AttrSpecBase] = attrs ++ foreignKeys

    lazy val colId = name.columnId

    lazy val valDefs = modelParams.map(_.valDef)

    def tildeFields = {
      modelParams map { _.tilde }
    }

    val sqlTableId = name.dp.toLowerCase

    def columns = modelParams.map { _.column }

    def tableBases: List[Tree] = Nil

    def modelBases: List[Tree] = Nil

    def queryBase = tq"$crudBase[$name, $tableName]"

    def queryType: Tree = tq"TableQuery"

    def queryExtra: List[Tree] = Nil

    def times = {
      q"""
      def * = (..$tildeFields).shaped <> (
        ($term.apply _).tupled, $term.unapply
      )
      """
    }

    def modelExtra: List[Tree] = Nil
  }

  class ModelOps(m: ModelSpec)
  extends TableOps[ModelSpec](m)
  {
    def assocTables = {
      assocs.map { ass ⇒
        val assType = TypeName(assocName(ass))
        val fks = List(
          ForeignKeySpec(TermName(name.d), name.tree),
          ForeignKeySpec(ass.singularTerm, ass.actualType)
        )
        AssocSpec(assType, fks)
      }
    }

    lazy val idColumn = IdColSpec()

    override def modelParams = {
      nonDateFields ++ dateColumns
    }

    def nonDateFields = super.modelParams ++ extraColumns

    override def modelExtra = {
      timestamps ? List(withDate) / Nil
    }

    def withDate = {
      val fields = nonDateFields map { _.paramName }
      q"""
      def withDate(u: DateTime) = {
        ${term}(..$fields, created = created, updated = u)
      }
      """
    }

    def extraColumns: List[AttrSpecBase] = List(idColumn)

    def dateColumns = {
      timestamps ? List(DateColSpec("created"), DateColSpec("updated")) / Nil
    }

    override def tableBases = List(tq"slick.db.TableEx[$name]")

    override def modelBases = List(
      Some(tq"slick.db.Model"),
      timestamps ? tq"slick.db.Timestamps[$name]"
    ).flatten ++ bases

    override def crudBase = {
      info.timestamps ? tq"slick.db.CrudEx" / tq"slick.db.Crud"
    }
  }

  class AssocOps(m: AssocSpec)
  extends TableOps[AssocSpec](m)

  abstract class TableSpec
  (implicit val info: BasicInfo)
  {
    def name: TypeName
    def params: List[AttrSpecBase]
    def bases: List[Tree]
  }

  case class ModelSpec(
    name: TypeName, params: List[AttrSpecBase], bases: List[Tree],
    body: List[Tree]
  )
  (implicit info: BasicInfo)
  extends TableSpec

  case class AssocSpec(name: TypeName, params: List[AttrSpecBase])
  (implicit info: BasicInfo)
  extends TableSpec
  {
    def bases = Nil
  }

  object ModelSpec
  {
    def parse(
      name: TypeName, params: List[ValDef], bases: List[Tree], body: List[Tree]
    )(implicit info: BasicInfo) = {
      val attrs = params map parseParam
      new ModelSpec(name, attrs, bases, body)
    }

    def parseParam(tree: ValDef)(implicit info: BasicInfo) = {
      val q"$mod val $name: $tpt = $default" = tree
      val model = info.isModel(AttrSpec.actualType(tpt))
      val assoc = AttrSpec.isAssoc(tpt)
      if (model) {
        if (assoc) AssociationSpec(name, tpt, default)
        else ForeignKeySpec(name, tpt)
      }
      else AttrSpec(name, tpt, default)
    }
  }

  case class SchemaSpec[A <: SchemaMacrosBase: EnumProcessor](
    comp: CompanionData,
    models: List[ModelSpec],
    enums: List[EnumSpec],
    misc: List[Tree],
    imports: List[Tree]
  )
  {
    def enum = enums map implicitly[EnumProcessor[A]].apply flatten
  }

  object SchemaSpec
  {
    def parse[A <: SchemaMacrosBase: EnumProcessor](comp: CompanionData)
    (implicit info: BasicInfo) =
    {
      val z = (List[ModelSpec](), List[EnumSpec](), List[Tree](), List[Tree]())
      val (models, enums, misc, imports) = comp.body.foldRight(z) {
        case (tree, (models, enums, misc, imports)) ⇒ tree match {
          case q"object $name extends Enumeration { ..$body }" ⇒
            (models, EnumSpec(name, body) :: enums, misc, imports)
          case q"case class $name(..$params) extends ..$bases { ..$body }" ⇒
            (parseModel(name, params, bases, body) :: models, enums, misc,
              imports)
          case i @ q"import $ref.{..$sels}" ⇒
            (models, enums, misc, i :: imports)
          case a ⇒
            (models, enums, a :: misc, imports)
        }
      }
      SchemaSpec[A](comp, models, enums, misc, imports)
    }

    def parseModel(
      name: TypeName, params: List[ValDef], bases: List[Tree], body: List[Tree]
    )(implicit info: BasicInfo) = {
      ModelSpec.parse(name, params, bases, body)
    }
  }

  case class BasicInfo(comp: CompanionData)
  {
    val models = comp.body.collect {
      case q"case class $name(..$fields) extends ..$bases { ..$body }" ⇒ name
    }

    def isModel(tpt: Tree) = {
      val tq"${name: TypeName}" = tpt
      models.contains(name)
    }

    val baseNames = comp.bases map { _.toString.split('.').last }

    def hasBase(name: String) = baseNames.contains(name)

    lazy val timestamps = hasBase("Timestamps")
  }
}
