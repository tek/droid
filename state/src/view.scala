package tryp
package droid
package state

import iota._

import IOOperation._
import AppState._

trait ViewDataI[A]
extends Data
{
  def view: A
}

trait ViewTrans
extends IOTrans
with Views[Context, StreamIO]
{
  override def machinePrefix = super.machinePrefix :+ "view"
}

trait ViewMachine
extends Machine
{
  def transitions(comm: MComm): ViewTrans
}

abstract class TreeViewTrans[A <: ViewTree[_ <: ViewGroup]: ClassTag]
extends ViewTrans
{
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

  protected def infMain: StreamIO[A, Context]

  protected def dataWithTree(data: Data, tree: A): Data =
    VData(tree)

  override def internalAdmit = super.internalAdmit orElse {
    case CreateContentView =>
      _ << infMain.map(ContentTree(_).back)
    case ContentTree(tree: A) => {
      case S(s, d) =>
        val data = dataWithTree(d, tree)
        S(s, data) << SetContentTree(tree, Some(sender)).toAgentMachine
    }
  }
}

trait TreeViewMachine
extends ViewMachine
{
  def transitions(comm: MComm): TreeViewTrans[_]
}

trait ViewAgentTrans
extends MachineTransitions
{
  def viewMachine: ViewMachine

  override def overrideAdmit = super.overrideAdmit orElse {
    case a: SetContentView => {
      _ << a.toParent
    }
    case a: SetContentTree => {
      _ << a.toParent
    }
    case CreateContentView => {
      case s =>
        s << CreateContentView.to(viewMachine)
    }
  }
}

trait ViewAgent
extends Agent
with Views[Context, StreamIO]
{ self =>
  def viewMachine: ViewMachine

  def machines: List[Machine] = viewMachine :: Nil

  def transitions(mc: MComm) = new ViewAgentTrans {
    def mcomm = mc
    def viewMachine = self.viewMachine
    def admit = PartialFunction.empty
  }
}
