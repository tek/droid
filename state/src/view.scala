package tryp
package droid
package state

import iota._

import IOOperation._
import AppState._

object ViewMachine
{
  def apply[A <: ViewGroup](lay: StreamIO[A, Context]) =
    new ViewMachine[A] {
      val layout = lay

      def admit: Admission = PartialFunction.empty
    }
}

trait ViewMachine[A <: ViewGroup]
extends IOMachine
with Views[Context, StreamIO]
{
  case class ContentView(view: A)
  extends Message

  case class ContentTree(tree: ViewTree[A])
  extends Message
  {
    override def toString = "ContentTree"
  }

  trait ViewData
  extends Data
  {
    def view: A
    def tree: Option[ViewTree[A]]
  }

  object ViewData
  {
    def unapply(a: ViewData) = Some((a.view, a.tree))
  }

  case class SimpleViewMachineData(view: A, tree: Option[ViewTree[A]])
  extends ViewData

  val Aid = iota.effect.Id

  override def machinePrefix = super.machinePrefix :+ "view"

  protected def dataWithView(data: Data, view: A): ViewData =
    SimpleViewMachineData(view, None)

  protected def dataWithTree(data: Data, tree: ViewTree[A]): ViewData =
    SimpleViewMachineData(tree.container, Some(tree))

  override def internalAdmit = super.internalAdmit orElse {
    case ContentView(view) => {
      case S(s, d) =>
        S(s, dataWithView(d, view)) <<
          SetContentView(view, Some(this)).toAgentMachine
    }
    case ContentTree(tree) => {
      case S(s, d) =>
        val data = dataWithTree(d, tree)
        S(s, data) << SetContentTree(tree, Some(this)).toAgentMachine
    }
  }
}

trait SimpleViewMachine
extends ViewMachine[ViewGroup]
{
  def admit: Admission = PartialFunction.empty
}

trait IOViewMachine[A <: ViewGroup]
extends ViewMachine[A]
{
  import AppState._
  import ViewMachine._

  def layout: StreamIO[A, Context]

  override def internalAdmit = super.internalAdmit orElse {
    case CreateContentView => {
      case s =>
        s << layout.map(ContentView(_).to(this))
    }
  }
}

trait ViewAgent[A <: ViewGroup]
extends Agent
with Views[Context, StreamIO]
with IOMachine
{
  def viewMachine: ViewMachine[A]

  override def machines = viewMachine :: super.machines

  override def extraAdmit = super.extraAdmit orElse {
    case a: SetContentView => {
      _ << a.toParent
    }
    case a: SetContentTree => {
      _ << a.toParent
    }
    case CreateContentView => {
      case s =>
        s << viewMachine.sendP(CreateContentView)
    }
  }
}

trait IOViewAgent[A <: ViewGroup]
extends ViewAgent[A]
{
  import iota.module.TextCombinators.text

  def viewMachine: IOViewMachine[A]

  def dummyLayout: StreamIO[View, Context] =
    (w[TextView] >>= text("Couldn't load content")) map(a => a: View)

  lazy val layout = viewMachine.layout

  // def setContentView = layout.map(v => act(_.setContentView(v)))
}

object ViewAgent
{
  def apply[A <: ViewGroup](lay: StreamIO[A, Context]) = new ViewAgent[A] {
    lazy val viewMachine = ViewMachine(lay)
  }
}

trait FreeViewAgent[A <: ViewGroup]
extends IOViewAgent[A]
{
  implicit def activity: Activity

  def title: String

  def safeViewP: Process[ZTask, View] = {
    Process.eval(layout.unsafePerformIO)
      .sideEffect { v =>
        log.debug(s"setting view for $title:\n${v.viewTree.drawTree}")
      }
  }

  def safeView: View = {
    safeViewP
      .infraRunLastFor("obtain layout", 10 seconds)
      .getOrElse(dummyLayout.unsafePerformIO.unsafePerformSync)
  }
}
