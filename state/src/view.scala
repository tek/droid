package tryp
package droid
package state

import iota._

import shapeless._

import tryp.state.annotation.cell

import MainViewMessages._

trait ViewDataI[A]
extends CState
{
  def tree: A

  def sub: CState
}

case class ViewCellData(view: CState, extra: CState)
extends CState

case class MainTree(tree: AnyTree)
extends Message
{
  override def toString = "MainTree"
}

trait ViewCellBase
extends AnnotatedTIO
with AnnotatedIO
with view.ViewToIO
{
  type CellTree <: AnyTree

  trait ViewData
  extends ViewDataI[CellTree]

  object ViewData
  {
    def apply(tree: CellTree, sub: CState): ViewData = VData(tree, sub)

    def unapply(a: CState) = a match {
      case a: ViewData => Some((a.tree, a.sub))
      case ViewCellData(a: ViewData, _) => Some((a.tree, a.sub))
      case _ => None
    }
  }

  case class VData(tree: CellTree, sub: CState)
  extends ViewData

  def infMain: IO[CellTree, Context]

  def stateWithTree(state: CState, tree: CellTree, sub: Option[CState], extra: Option[CState]): CState = state match {
    case ViewCellData(ViewData(_, s), e) => ViewCellData(VData(tree, sub | s), extra | e)
    case ViewData(_, s) => ViewCellData(VData(tree, sub | s), extra | Pristine)
    case _ => ViewCellData(VData(tree, sub | Pristine), extra | Pristine)
  }

  def narrowTree(tree: AnyTree): Option[CellTree]

  object CellTree
  {
    def unapply(tree: AnyTree): Option[CellTree] = narrowTree(tree)
  }
}

@cell
trait ViewCell
extends ViewCellBase
{
  def trans: Transitions = {
    case CreateMainView => ContextIO(infMain.map(MainTree(_))).broadcast :: HNil
    case MainTree(CellTree(tree)) => {
      case s => stateWithTree(s, tree, None, None) :: SetMainTree(tree).broadcast :: HNil
    }
  }
}
