package tryp
package droid
package state

import view._

import concurrent.duration._

import scalaz._, Scalaz._, stream.async

object ViewMachine
{
  case class Layout[A <: View](layout: IOB[A])
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

abstract class ViewMachine[A <: View](implicit ctx: AndroidUiContext,
  mt: MessageTopic, val res: Resources)
extends SimpleDroidMachine
with ExtViews
with TextCombinators
{
  import ViewMachine._

  val Aid = iota.Id

  def handle = "view"

  def layoutIOT: IOB[A]

  lazy val layout: async.mutable.Signal[IOB[View]] =
    async.signalUnset[IOB[View]]

  def admit: Admission = {
    case Create(_, _) ⇒ create
    case SetLayout ⇒ setLayout
  }

  val create: Transit = {
    case S(Pristine, _) ⇒
      val data = Layout(layoutIOT)
      S(Initialized, data) << SetLayout
  }

  val setLayout: Transit = {
    case s @ S(_, Layout(l)) ⇒
      s << stateEffectTask[Task, Unit]("set layout signal")(layout.set(l))
  }
}
