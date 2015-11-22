package tryp
package droid

import view._

import concurrent.duration._

import scalaz._, Scalaz._, concurrent._, stream._
import concurrent.Task

import State._

object ViewState
{
  case class Layout(layout: IOT[View])
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

abstract class ViewState(implicit ec: EC, ctx: AndroidUiContext,
  mt: MessageTopic, val res: Resources)
extends DroidStateEC
with ExtViews
with TextCombinators
{
  import ViewState._

  val Aid = iota.Id

  def handle = "view"

  def transitions: ViewTransitions = {
    case Create(_, _) ⇒ create
    case SetLayout ⇒ setLayout
  }

  val create: ViewTransition = {
    case S(Pristine, _) ⇒
      val data = Layout(layoutIOT)
      S(Initialized, data) << SetLayout
  }

  val setLayout: ViewTransition = {
    case s @ S(_, Layout(l)) ⇒
      s << stateEffectTask("set layout signal")(layout.set(l))
  }

  def layoutIOT: IOT[View]

  lazy val layout = async.signalUnset[IOT[View]]
}
