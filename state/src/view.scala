package tryp
package droid
package state

import iota._

import shapeless._

import tryp.state.annotation.machine

import MainViewMessages._

object ViewMachineTypes
{
  type AnyTree = ViewTree[_ <: ViewGroup]
}
import ViewMachineTypes._

case class ToViewMachine(payload: Message, agent: Machine)
extends InternalMessage

trait ViewDataI[A]
extends MState
{
  def view: A

  def sub: MState
}

case class MainTree(tree: AnyTree)
extends Message
{
  override def toString = "MainTree"
}


trait ViewMachineBase[A <: AnyTree]
extends AnnotatedTIO
with view.ViewToIO
{
  abstract class ViewData
  extends ViewDataI[A]

  object ViewData
  {
    def unapply(a: ViewData) = Some((a.view, a.sub))
  }

  case class VData(view: A, sub: MState)
  extends ViewData

  def infMain: IO[A, Context]

  def stateWithTree(state: MState, tree: A): MState = state match {
    case ViewData(_, sub) => VData(tree, sub)
    case _ => VData(tree, Pristine)
  }
}

@machine
abstract class ViewMachine[A <: AnyTree: ClassTag]
extends ViewMachineBase[A]
{
  def trans: Transitions = {
    case CreateMainView => ContextIO(infMain.map(MainTree(_))) :: HNil
    case MainTree(tree: A) => {
      case s => stateWithTree(s, tree) :: SetMainTree(tree) :: HNil
    }
  }
}
