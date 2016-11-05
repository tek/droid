package tryp
package droid
package state

import iota._

import tryp.state._

import android.view.ViewGroup.LayoutParams
import android.widget._

import view._
import view.core._
import io.text._
import io.misc._
import AppState._

class MainFrame(c: Context)
extends FrameLayout(c)
with Logging
{
  override def addView
  (v: View, index: Int, params: LayoutParams): Unit =
  {
    if (getChildCount != 0)
      log.error(s"Tried to add child $v to MainFrame $toString" +
        " which already has children")
    else super.addView(v, index, params)
  }

  def setView(a: View) = {
    if (getChildCount != 0) removeViewAt(0)
    addView(a)
  }
}

object MainViewMessages
{
  case class LoadUi(agent: ViewAgent[_ <: ViewGroup])
  extends Message

  case class LoadContent(view: View)
  extends Message

  case class LoadFragment(fragment: () => Fragment, tag: String)
  extends Message

  case object MainViewReady
  extends Message

  case object MainViewLoaded
  extends Message

  case object CreateMainView
  extends Message

  case class SetMainView[A <: ViewGroup](view: StreamIO[A, Context])
  extends Message

  case class SetMainTree[A <: ViewGroup](tree: ViewTree[A])
  extends Message

  case object Back
  extends Message

  case object UiLoaded
  extends Message

  case object InitUi
  extends Message
}
import MainViewMessages._

@Publish(LoadUi)
trait MVContainer
extends IOViewMachine[ViewGroup]
{
  def admit: Admission = {
    case SetContentView(view, _) => setContentView(view)
    case SetContentTree(tree, _) => setContentView(tree.container)
    case Back => back
    case ContentViewReady(agent) => contentViewReady
    case UiLoaded => uiLoaded
  }

  import IOOperation.exports._

  // If the main frame has already been installed (state Ready), immediately
  // request the main view
  def uiLoaded: Transit = {
    case s @ S(Ready, _) =>
      s << InitUi.toLocal
  }

  /** `view` has to be executed before its signal can be used, so the effect
   *  has to be a StreamIO IOTask, which produces a ViewStreamTask of `content`
   *  setting its view to `view`'s result.
   */
  def setContentView(view: View): Transit = {
    case s =>
      s << content.v
        .map(_.setView(view))
        .map(_ => MainViewLoaded.publish)
        .main

  }

  def contentViewReady: Transit = {
    case S(_, d) =>
      S(Ready, d) << InitUi.toLocal
  }

  def back: Transit = {
    case s =>
      s << nativeBack
  }

  // FIXME must be overridden in activity to delegate to here. then implement
  // and call nativeBack() in activity
  def nativeBack = act(_.onBackPressed())

  override def machineName = "main".right

  override def description = "main view"

  lazy val content = w[MainFrame] >>- metaName[MainFrame]("content frame")

  lazy val layout =
    l[FrameLayout](content).widen[ViewGroup] >>- metaName("root frame") >>-
      bgCol("main")
}

trait MV
extends IOMachine
{
  case class MVData(ui: Agent)
  extends Data

  def admit: Admission = {
    case LoadUi(ui) => loadUi(ui)
    case InitUi => initUi
  }

  def loadUi(ui: ViewAgent[_ <: ViewGroup]): Transit = {
    case S(s, _) =>
      S(s, MVData(ui)) << AgentStateData.AddSub(Nel(ui)).toAgentMachine <<
        UiLoaded.toLocal
  }

  def initUi: Transit = {
    case s @ S(_, MVData(agent)) =>
      s << CreateContentView.to(agent)
  }
}

trait MainViewAgent
extends ActivityAgent
{
  import AppState._

  lazy val viewMachine = new MVContainer {
  }

  lazy val mvMachine = new MV {
  }

  override def machines = mvMachine :: super.machines

  private[this] def setContentViewAdmit: Admission = {
    case m @ SetContentView(view, Some(sender)) if sender == viewMachine =>
      _ << m.toParent
    case m @ SetContentView(_, _) =>
      _ << m.to(viewMachine)
    case m @ SetContentTree(view, Some(sender)) if sender == viewMachine =>
      _ << m.toParent
    case m @ SetContentTree(_, _) =>
      _ << m.to(viewMachine)
  }

  override def extraAdmit = setContentViewAdmit orElse super.extraAdmit

  override def admit = super.admit orElse {
    case ContentViewReady(ag) if ag == this =>
      _ << MainViewReady.toLocal
  }
}
