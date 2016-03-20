package tryp
package droid
package state

import view._

import concurrent.duration._

import scalaz._, Scalaz._, stream.async

import shapeless.tag.@@

object ViewMachine
{
  case class Layout[A <: View](layout: FreeIO[A])
  extends Data

  case object SetLayout
  extends Message
}

// TODO
// to decouple from android activity/context/fragment:
// - inject UiContext into state from onCreate
//   maybe use different data (or substates – state tree/seq?) for different
//   ui contexts
// - always return an empty view (maybe progress spinner) in onCreateView et al
//   set the view asynchronously from the state by manual calls to addView()
//   this requires smart reactions to lifecycle events
//   in subviews (nested fragments, maybe reinvent), pass a modified UiContext
//   that contains a reference to the view root for insertion

trait ViewMachine
extends Machine
with ExtViews
{
  import ViewMachine._

  val Aid = iota.Id

  def handle = "view"

  override protected def initialZ = S(Initialized, Layout(layoutIO))

  override protected def initialMessages = Process.emit(SetLayout)

  lazy val layout: async.mutable.Signal[FreeIO[_ <: View]] =
    async.signalUnset[FreeIO[_ <: View]]

  def layoutIO: FreeIO[_ <: View]

  def admit: Admission = {
    case SetLayout => setLayout
  }

  val setLayout: Transit = {
    case s @ S(_, Layout(l)) =>
      s << layout.setter(l).effect("set layout signal")
  }
}
