package tryp
package droid
package unit

import reflect.macros.blackbox

import scalaz._, Scalaz._

class FragSpecAnn(val c: blackbox.Context)
extends SimpleAnnotation
{
  import c.universe._

  def apply(annottees: Annottees) = {
    val frag = params.headOption.getOrAbort("no fragment class specified")
    val act = TypeName(frag.toString).suffix("Activity")
    val compName = annottees.comp.term
    val actCls = List[Tree](
      q"""
      class $act
      extends SpecActivity
      {
        override def frag = $frag.apply
      }
      """
      )
    val actCtor = List[Tree](
      q"def activityClass = classOf[$compName.$act]"
    )
    val bases = List(tq"ActivitySpec[$compName.$act]", tq"DefaultStrategy")
    val l1 = (Annottees.cls ^|-> ClassData.bases).append(bases)
    val l2 = (Annottees.cls ^|-> ClassData.body ^|-> BodyData.misc)
      .append(actCtor)
    val l3 = (Annottees.comp ^|-> ModuleData.body ^|-> BodyData.misc)
      .append(actCls)
    l3(l2(l1(annottees)))
  }
}

@anno(FragSpecAnn) class frag
