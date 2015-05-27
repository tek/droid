package slick.test

import scala.reflect.macros.whitebox.Context

import slick._

class TestSchema
extends annotation.StaticAnnotation
{
  def macroTransform(annottees: Any*): Any = macro SchemaMacrosTest.process
}

class SchemaMacrosTest(val c: Context)
extends SchemaMacrosBase
{
  import c.universe._

  implicit def modelOps(cls: ModelSpec) = new ModelOps(cls)

  implicit object TestEnumProcessor
  extends BasicEnumProcessor[SchemaMacrosTest]

  object SchemaTestTransformer
  extends Transformer
  {
    def apply(cls: ClassData, comp: CompanionData) = {
      implicit val info = BasicInfo(comp)
      val schema = SchemaSpec.parse(comp)
      val modelComps = info.models map { a ⇒ TermName(a.toString) }
      val alphaType = AttrSpec.actualType(schema.models.head.tpe)
      val gammaModel = schema.models.last
      val assocType = AttrSpec.actualType(gammaModel.params.head.tpt)
      val optAttr = gammaModel.params(1)
      val optType = AttrSpec.actualType(optAttr.tpt)
      val alphaIsModel = info.isModel(optType)
      val alpFk = gammaModel.foreignKeys.last
      val betaAssoc = gammaModel.assocTables.head
      val betaAssocKey1 = betaAssoc.params(0)
      val betaAssocKey2 = betaAssoc.params(1)
      val betaAttr = gammaModel.assocs.head
      val body = q"""
      object Original
      {
        ..${comp.body}
      }
      import Original._

      "info" >> {
        $modelComps must_== List(Alpha, Beta, Gamma)
      }

      "actualType" >> {
        classOf[$alphaType] must_== classOf[Alpha]
        classOf[$assocType] must_== classOf[Beta]
        classOf[$optType] must_== classOf[Alpha]
        ${optAttr.option} must beTrue
        $alphaIsModel must beTrue
        ${betaAssocKey1.name.toString} must_== "gamma"
        ${betaAssocKey2.name.toString} must_== "bet"
        ${betaAttr.query} must_== Beta
        ${alpFk.colName.toString} must_== "alpId"
        ${alpFk.sqlFk} must_== "alp"
        ${alpFk.sqlColId} must_== "alp_id"
      }
      """
      val bases = tq"org.specs2.mutable.Specification" :: Nil
      (cls, comp.copy(bases = bases, body = body.children))
    }
  }

  val transformers = SchemaTestTransformer :: Nil
}
