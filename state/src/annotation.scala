package tryp
package droid
package state

import scala.reflect.macros.blackbox

import tryp.macros.SimpleAnnotation

trait CellHelperAnnotation
extends SimpleAnnotation
{
  import c.universe._

  def setupCell: Annottees => Annottees = {
    val mainL = classOrModuleLens
    val annoL = mainL composeLens lens_ImplDef_annotations
    val parentsL = mainL composeLens lens_ImplDef_parents
    val annoTrans = annoL.appendMap { a =>
      if (a.map(parseAnnotation).exists(_.name == "cell")) Nil
      else List(q"new tryp.state.annotation.cell")
    }
    val tio = symbolOf[AnnotatedTAIO]
    val parentTrans = parentsL.appendMap { p =>
      val present = p.exists { t => c.typecheck(t, mode = c.TYPEmode).tpe == tio.toType }
      if (present) Nil else List(symbolOf[AnnotatedTAIO].tree)
    }
    annoTrans andThen parentTrans
  }
}

class ViewCellM(val c: blackbox.Context)
extends CellHelperAnnotation
{
  import c.universe._

  def apply(a0: Annottees): Annottees = {
    val cellTree = typeArgs match {
      case List(ct) => ct
      case _ => abort("@viewCell needs one type argument, `CellTree`")
    }
    val cellTreeDef = q"type CellTree = $cellTree"
    val inflateDef = q"def infMain = inflate[$cellTree]"
    val narrow =
      q"""
      def narrowTree(tree: droid.state.AnyTree) = tree match {
        case t: CellTree => Some(t)
        case _ => None
      }
      """
    val mainL = classOrModuleLens
    val bodyL = mainL composeLens lens_ImplDef_body
    val trans = setupCell andThen bodyL.prepend(List(cellTreeDef, inflateDef, narrow))
    trans(a0)
  }
}

class CellModelM(val c: blackbox.Context)
extends CellHelperAnnotation
{
  import c.universe._

  def apply(a0: Annottees): Annottees = {
    val model = typeArgs match {
      case List(ct) => ct
      case _ => abort("@cellModel needs one type argument, the cell model class")
    }
    val cmodel = symbolOf[tryp.state.ext.CModel[_]].companion
    val modelEx =
      q"""
      object Model
      {
        def unapply(a: Any): Option[$model] = a match {
          case model: $model => Some(model)
          case _ => None
        }
      }
      """
    val cmodelEx =
      q"""
      object CM
      {
        def unapply(s: CState): Option[$model] = s match {
          case $cmodel(Model(model)) => Some(model)
          case _ => None
        }
      }
      """
    val mainL = classOrModuleLens
    val bodyL = mainL composeLens lens_ImplDef_body
    val trans = setupCell andThen bodyL.prepend(List(modelEx, cmodelEx))
    trans(a0)
  }
}

object annotation
{
  @anno(ViewCellM) class viewCell
  @anno(CellModelM) class cellModel
}
