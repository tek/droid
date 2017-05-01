package tryp
package droid
package state

import iota._

import shapeless._

import tryp.state.annotation.cell

import MainViewMessages._

case class ToViewCell(payload: Message, agent: Cell)
extends InternalMessage

trait ViewDataI[A]
extends CState
{
  def tree: A

  def sub: CState
}

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

  abstract class ViewData
  extends ViewDataI[CellTree]

  object ViewData
  {
    def apply(tree: CellTree, sub: CState) = VData(tree, sub)

    def unapply(a: ViewData) = Some((a.tree, a.sub))
  }

  case class VData(tree: CellTree, sub: CState)
  extends ViewData

  def infMain: IO[CellTree, Context]

  def stateWithTree(state: CState, tree: CellTree, sub: Option[CState]): CState = state match {
    case ViewData(_, s) => VData(tree, sub | s)
    case _ => VData(tree, sub | Pristine)
  }

  def narrowTree(tree: AnyTree): Option[CellTree]

  object CellTree
  {
    def unapply(tree: AnyTree): Option[CellTree] = narrowTree(tree)
  }
}

@cell
abstract class ViewCell
extends ViewCellBase
{
  def trans: Transitions = {
    case CreateMainView => ContextIO(infMain.map(MainTree(_))).broadcast :: HNil
    case MainTree(CellTree(tree)) => {
      case s => stateWithTree(s, tree, None) :: SetMainTree(tree).broadcast :: HNil
    }
  }
}
