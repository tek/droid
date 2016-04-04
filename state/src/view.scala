package tryp
package droid
package state

import view._
import view.core._
import state.core._

import concurrent.duration._

import scalaz._, Scalaz._, stream.async

import shapeless.tag.@@

object ViewMachine
{
  case class Layout[A <: View](layout: StreamIO[A, Context])
  extends Data

  case object SetLayout
  extends Message
}

trait ViewMachine
extends Machine
with Views[Context, StreamIO]
{
  import ViewMachine._

  val Aid = iota.Id

  override def handle = "view"

  override protected def initialZ = S(Initialized, Layout(layoutIO))

  override protected def initialMessages =
    super.initialMessages ++ Process.emit(SetLayout)

  lazy val layout = async.signalUnset[StreamIO[View, Context]]

  def layoutIO: StreamIO[_ <: View, Context]

  def admit: Admission = {
    case SetLayout => setLayout
  }

  val setLayout: Transit = {
    case s @ S(_, Layout(l)) =>
      s << layout.setter(l).stateSideEffect("set layout signal")
  }
}
