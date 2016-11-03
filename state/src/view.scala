package tryp
package droid
package state

import view._
import view.core._

import scalaz.stream.async

import IOOperation._

object ViewMachine
{
  def apply(lay: StreamIO[_ <: View, Context]) =
    new ViewMachine {
      val layout = lay

      def admit: Admission = PartialFunction.empty
    }
}

trait ViewMachine
extends IOMachine
with Views[Context, StreamIO]
{
  import ViewMachine._

  val Aid = iota.effect.Id

  override def machinePrefix = super.machinePrefix :+ "view"

  def layout: StreamIO[_ <: View, Context]
}

trait SimpleViewMachine
extends ViewMachine
{
  def admit: Admission = PartialFunction.empty
}

trait IOViewMachine
extends ViewMachine
{
  import AppState._

  override def extraAdmit = super.extraAdmit orElse {
    case CreateContentView => {
      case s =>
        s << layout.map(SetContentView(_))
    }
  }
}

trait ViewAgent
extends Agent
with Views[Context, StreamIO]
with IOMachine
{
  import AppState._

  def viewMachine: ViewMachine

  override def machines = viewMachine :: super.machines

  import iota.module.TextCombinators.text

  def dummyLayout: StreamIO[View, Context] =
    (w[TextView] >>= text("Couldn't load content")) map(a => a: View)

  lazy val layout = viewMachine.layout

  def setContentView = layout.map(v => act(_.setContentView(v)))
}

trait IOViewAgent
extends ViewAgent
{
}

object ViewAgent
{
  def apply(lay: StreamIO[_ <: View, Context]) = new ViewAgent {
    lazy val viewMachine = ViewMachine(lay)
  }
}

trait FreeViewAgent
extends ViewAgent
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
