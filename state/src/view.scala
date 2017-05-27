package tryp
package droid
package state

import iota._

import shapeless.::

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
      case a: ViewData @unchecked => Some((a.tree, a.sub))
      case ViewCellData(a: ViewData @unchecked, _) => Some((a.tree, a.sub))
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

  case class InsertTree(tree: CellTree)
  extends Message

  // Those need to be defined here, or we get a cyclic reference
  case object TreeInserted
  extends Message

  def treeCreated(s: CState, tree: CellTree) =
    stateWithTree(s, tree, None, None) :: InsertTree(tree).local :: HNil

  def insertTree[M <: Message](msg: M) = msg :: TreeInserted :: HNil

  case object CreateTree
  extends Message

  case class TreeCreated(tree: CellTree)
  extends Message

  def createTree: ContextIO = ContextIO(infMain.map(TreeCreated(_)))
}

@cell
trait ViewCell
extends ViewCellBase
{
  def trans: Transitions = {
    case CreateTree => createTree :: HNil
    case TreeCreated(tree) => { case s => tree match { case CellTree(t) => Some(treeCreated(s, tree)) case _ => None } }
  }
}
