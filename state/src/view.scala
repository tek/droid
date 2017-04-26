package tryp
package droid
package state

import iota._

import shapeless._

import tryp.state.annotation.cell

import MainViewMessages._

object ViewCellTypes
{
  type AnyTree = ViewTree[_ <: ViewGroup]
}
import ViewCellTypes._

case class ToViewCell(payload: Message, agent: Cell)
extends InternalMessage

trait ViewDataI[A]
extends CState
{
  def view: A

  def sub: CState
}

case class MainTree(tree: AnyTree)
extends Message
{
  override def toString = "MainTree"
}


trait ViewCellBase[A <: AnyTree]
extends AnnotatedTIO
with view.ViewToIO
{
  abstract class ViewData
  extends ViewDataI[A]

  object ViewData
  {
    def unapply(a: ViewData) = Some((a.view, a.sub))
  }

  case class VData(view: A, sub: CState)
  extends ViewData

  def infMain: IO[A, Context]

  def stateWithTree(state: CState, tree: A): CState = state match {
    case ViewData(_, sub) => VData(tree, sub)
    case _ => VData(tree, Pristine)
  }
}

@cell
abstract class ViewCell[A <: AnyTree: ClassTag]
extends ViewCellBase[A]
{
  def trans: Transitions = {
    case CreateMainView => ContextIO(infMain.map(MainTree(_))) :: HNil
    case MainTree(tree: A) => {
      case s => stateWithTree(s, tree) :: SetMainTree(tree) :: HNil
    }
  }
}
