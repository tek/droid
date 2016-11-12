package tryp
package droid
package state

import iota._

import IOOperation._
import AppState._

object ViewMachine
{
  def apply[A <: ViewGroup](lay: StreamIO[A, Context]) =
    new IOViewMachine[A] {
      val layout = lay

      def admit: Admission = PartialFunction.empty
    }
}

trait ViewDataI[A]
extends Data
{
  def view: A
}

trait ViewMachine
extends IOMachine
with Views[Context, StreamIO]
{
  override def machinePrefix = super.machinePrefix :+ "view"
}

abstract class TreeViewMachine[A <: ViewTree[_ <: ViewGroup]: ClassTag]
extends ViewMachine
{
  case class ContentTree(tree: A)
  extends Message
  {
    override def toString = "ContentTree"
  }

  trait ViewData
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

  protected def dataWithTree(data: Data, tree: A): ViewData =
    VData(tree)

  override def internalAdmit = super.internalAdmit orElse {
    case CreateContentView =>
      _ << infMain.map(ContentTree(_).back)
    case ContentTree(tree: A) => {
      case S(s, d) =>
        val data = dataWithTree(d, tree)
        S(s, data) << SetContentTree(tree, Some(this)).toAgentMachine
    }
  }
}

trait IOViewMachine[A <: ViewGroup]
extends ViewMachine
{
  import AppState._
  import ViewMachine._

  def layout: StreamIO[A, Context]

  case class ContentView(view: A)
  extends Message

  trait ViewData
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

  protected def dataWithView(data: Data, view: A): ViewData =
    VData(view)

  override def internalAdmit = super.internalAdmit orElse {
    case ContentView(view) => {
      case S(s, d) =>
        S(s, dataWithView(d, view)) <<
          SetContentView(view, Some(this)).toAgentMachine
    }
    case CreateContentView => {
      case s =>
        s << layout.map(ContentView(_).back)
    }
  }
}

trait SimpleViewMachine
extends IOViewMachine[ViewGroup]
{
  def admit: Admission = PartialFunction.empty
}

trait ViewAgent
extends Agent
with Views[Context, StreamIO]
{
  def viewMachine: ViewMachine

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
extends ViewAgent
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
  def apply[A <: ViewGroup](lay: StreamIO[A, Context]) = new ViewAgent {
    lazy val viewMachine = ViewMachine[A](lay)
  }
}

trait FreeViewAgent[A <: ViewGroup]
extends IOViewAgent[A]
{
  implicit def activity: Activity

  def title: String

  def safeViewP: Process[ZTask, View] = {
    Process.eval(layout.unsafePerformIO)
      .sideEffect { case v =>
        log.debug(s"setting view for $title:\n${v.viewTree.drawTree}")
      }
  }

  def safeView: View = {
    safeViewP
      .infraRunLastFor("obtain layout", 10 seconds)
      .getOrElse(dummyLayout.unsafePerformIO.unsafePerformSync)
  }
}
