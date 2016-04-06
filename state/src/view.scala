package tryp
package droid
package state

import view._
import view.core._
import state.core._

import scalaz.stream.async

import shapeless.tag.@@

object ViewMachine
{
  trait Layout[A <: View]
  extends Data
  {
    def layout: StreamIO[A, Context]
  }

  object Layout
  {
    def unapply[A <: View](o: Layout[A]) = Some(o.layout)
  }

  case class ViewMachineData[A <: View](layout: StreamIO[A, Context])
  extends Layout[A]

  case object SetLayout
  extends Message

  case object LayoutReady
  extends Message
}

trait ViewMachine
extends Machine
with Views[Context, StreamIO]
{
  import ViewMachine._

  val Aid = iota.Id

  override def machinePrefix = super.machinePrefix :+ "view"

  override protected def initialZ = S(Initialized, ViewMachineData(layoutIO))

  override protected def initialMessages =
    super.initialMessages ++ Process.emit(SetLayout)

  lazy val layout = async.signalUnset[StreamIO[View, Context]]

  def layoutIO: StreamIO[_ <: View, Context]

  def admit: Admission = {
    case SetLayout => setLayout
  }

  val setLayout: Transit = {
    case s @ S(_, Layout(l)) =>
      s << layout.setter(l map(v => (v: View)))
        .stateSideEffect("set layout signal") << LayoutReady
  }
}

trait ViewAgent
extends Agent
with Views[Context, StreamIO]
{
  def viewMachine: ViewMachine

  override def machines = viewMachine %:: super.machines

  import iota.std.TextCombinators.text

  def dummyLayout: StreamIO[View, Context] =
    (w[TextView] >>= text("Couldn't load content")) map(a => a: View)

  def viewIO = viewMachine.layout.discrete.take(1)

  def safeViewIO: Process[Task, StreamIO[View, Context]] = {
    viewIO.availableOrHalt
  }
}

object ViewAgent
{
  def apply(lay: StreamIO[_ <: View, Context]) = new ViewAgent {
    lazy val viewMachine = new ViewMachine {
      val layoutIO = lay
    }
  }
}

trait FreeViewAgent
extends ViewAgent
{
  implicit def activity: Activity

  def title: String

  def safeViewP: Process[Task, View] = {
    safeViewIO
      .map(_.unsafePerformIO)
      .eval
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
