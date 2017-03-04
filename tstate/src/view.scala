package tryp
package droid
package state

import iota._

import shapeless._

import tryp.state.annotation.machine

case class ToViewMachine(payload: Message, agent: Machine)
extends InternalMessage

trait ViewDataI[A]
extends MState
{
  def view: A

  def sub: MState
}

@machine
abstract class ViewMachine[A <: ViewTree[_ <: ViewGroup]]
extends Views[Context, IO]
{
  case class ContentTree(tree: A)
  extends Message
  {
    override def toString = "ContentTree"
  }

  abstract class ViewData
  extends ViewDataI[A]

  object ViewData
  {
    def unapply(a: ViewData) = Some((a.view, a.sub))
  }

  case class VData(view: A, sub: MState)
  extends ViewData

  protected def infMain: IO[A, Context]

  protected def stateWithTree(state: MState, tree: A): MState = VData(tree, Pristine)

  def vmTrans: Transitions = {
    case CreateContentView => infMain.map(ContentTree(_).back) :: HNil
    case ContentTree(tree) => {
      case s => stateWithTree(s, tree) :: SetContentTree(tree, Some(this)).toAgentMachine :: HNil
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
    case CreateContentView => ToViewMachine(CreateContentView, this) :: HNil
  }
}
