package tryp
package droid
package recycler

import scala.reflect.macros.blackbox

import tryp.macros.SimpleAnnotation
import state.CellHelperAnnotation

class RVCellM(val c: blackbox.Context)
extends CellHelperAnnotation
{
  import c.universe._

  def apply(a0: Annottees): Annottees = {
    val (model, element) = typeArgs match {
      case List(m, e) => m -> e
      case _ => abort("@viewCell needs two type arguments, `Model` and `Element`")
    }
    val modelDef = q"type Model = $model"
    val elementDef = q"type Element = $element"
    val inflateDef = q"def infElem = inflate[$element]"
    val mainL = classOrModuleLens
    val bodyL = mainL composeLens lens_ImplDef_body
    val annoL = mainL composeLens lens_ImplDef_annotations
    val parentsL = mainL composeLens lens_ImplDef_parents
    val trans = setupCell andThen bodyL.prepend(List(modelDef, elementDef, inflateDef))
    trans(a0)
  }
}

object annotation
{
  @anno(RVCellM) class rvCell
}
