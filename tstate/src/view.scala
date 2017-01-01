package tryp
package droid
package state

import iota._

import shapeless._

import tryp.state.annotation.machine

case object State1
extends MState

case class ToViewMachine(payload: Message)
extends Parcel

trait ViewDataI[A]
extends MState
{
  def view: A
}

trait ViewMachineBase
extends Views[Context, IO]
{
}

@machine
abstract class ViewMachine[A <: ViewTree[_ <: ViewGroup]: ClassTag]
extends ViewMachineBase
{
  type Accepts = ToViewMachine :: HNil

  case class ContentTree(tree: A)
  extends Message
  {
    override def toString = "ContentTree"
  }

  abstract class ViewData
  extends ViewDataI[A]
  {
    def view: A
  }

  object ViewData
  {
    def unapply(a: ViewData) = Some(a.view)
  }

  case class VData(view: A)
  extends ViewData

  protected def infMain: IO[A, Context]

  protected def stateWithTree(state: MState, tree: A): MState =
    VData(tree)

  def vmTrans: Transitions = {
    case CreateContentView => {
      infMain.map(ContentTree(_).back) :: HNil
    }
    case ContentTree(tree: A) => {
      case s =>
        stateWithTree(s, tree) ::
          SetContentTree(tree, Some(this)).toAgentMachine :: HNil
    }
  }
}

@machine
trait ViewAgent
{
  def trans: Transitions = {
    // case a: SetContentView => {
    //   _ << a.toParent
    // }
    // case a: SetContentTree => {
    //   _ << a.toParent
    // }
    case CreateContentView => ToViewMachine(CreateContentView) :: HNil
  }
}
