package tryp
package droid
package state

import android.view.ViewGroup.LayoutParams
import android.widget._

import view._
import view.core._
import io.text._
import io.misc._
import state.core._

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
  case class LoadUi(agent: ViewAgent)
  extends Message

  // case class LoadMUi(agent: Ui[View])
  // extends Message

  case class LoadContent(view: View)
  extends Message

  case class LoadFragment(fragment: () => Fragment, tag: String)
  extends Message

  // case class ContentLoaded(view: Ui[View])
  // extends Message

  case class SetMainView[A <: View](view: StreamIO[A, Context])
  extends Message

  case object Back
  extends Message
}
import MainViewMessages._

@Publish(LoadUi)
trait MainViewMachine
extends ViewMachine
{
  import AppState._

  def admit: Admission = {
    case LoadUi(ui) => loadUi(ui)
    case SetMainView(view) => setMainView(view)
    // case ContentLoaded(view) => contentLoaded(view)
    case Back => back
  }

  def loadUi(agent: ViewAgent): Transit = {
    case s =>
      s << AgentStateData.AddSub(Nes(agent)).toAgent <<
        SetMainView(agent.layout).toResult
  }

  // def contentLoaded(view: Ui[View]): Transit = {
  //   case s =>
  //     s << ContextTask(ctx =>
  //         LogInfo(s"Loaded content view:\n${ctx.showViewTree(view.get)}")
  //   )
  // }

  import IOOperation.exports._

  /** `view` has to be executed before its signal can be used, so the effect
   *  has to be a StreamIO IOTask, which produces a ViewStreamTask of `content`
   *  setting its view to `view`'s result.
   */
  def setMainView(view: StreamIO[_ <: View, Context]): Transit = {
    case s =>
      s << view.map { v => content.v.map(_.setView(v)).main }
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
    l[FrameLayout](content) >>- metaName("root frame") >>- bgCol("main")
}

trait MainViewAgent
extends ActivityAgent
{
  lazy val viewMachine = new MainViewMachine {
  }
}
