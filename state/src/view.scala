package tryp
package droid
package state

import view._
import view.core._

import scalaz.stream.async

import shapeless.tag.@@

object ViewMachine
{
  def apply(lay: StreamIO[_ <: View, Context]) =
    new ViewMachine {
      val layout = lay

      def admit: Admission = PartialFunction.empty
    }
}

trait ViewMachine
extends Machine
with Views[Context, StreamIO]
{
  import ViewMachine._

  val Aid = iota.Id

  override def machinePrefix = super.machinePrefix :+ "view"

  def layout: StreamIO[_ <: View, Context]
}

trait SimpleViewMachine
extends ViewMachine
{
  def admit: Admission = PartialFunction.empty
}

trait ViewAgent
extends Agent
with Views[Context, StreamIO]
with Machine
{
  def viewMachine: ViewMachine

  override def machines = viewMachine :: super.machines

  import iota.std.TextCombinators.text

  def dummyLayout: StreamIO[View, Context] =
    (w[TextView] >>= text("Couldn't load content")) map(a => a: View)

  lazy val layout = viewMachine.layout

  def setContentView = layout.map(v => act(_.setContentView(v)))
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
