package tryp
package droid
package state

import iota._

import tryp.state._

import android.view.ViewGroup.LayoutParams
import android.widget._

import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle

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

trait HasMainFrame
{
  def mainFrame: MainFrame
}

case class MainViewLayout(container: MainFrame)
extends ViewTree[MainFrame]
with HasMainFrame
{
  override def toString = "MainViewLayout"

  def mainFrame = container
}

object MainViewMessages
{
  case class LoadUi(agent: ViewAgent)
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
abstract class MVContainer
[A <: ViewTree[_ <: ViewGroup] with HasMainFrame: ClassTag]
extends TreeViewMachine[A]
{
  def admit: Admission = {
    case SetContentView(view, _) => setMainView(view)
    case SetContentTree(tree, _) => setMainView(tree.container)
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
  def setMainView(view: View): Transit = {
    case s @ S(_, ViewData(main)) =>
      s << cio(_ => main.mainFrame.setView(view))
        .map(_ => MainViewLoaded.publish)
        .ui
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

  override def handle = "mvc"

  override def description = "mv container"
}

trait MVFrame
extends MVContainer[MainViewLayout]
{
  def infMain = inf[MainViewLayout]
}

case class MVData(ui: Agent)
extends Data

trait MV
extends IOMachine
{
  override def handle = "mv"

  override def description = "mv"

  def admit: Admission = {
    case LoadUi(ui) => loadUi(ui)
    case InitUi => initUi
  }

  def loadUi(ui: ViewAgent): Transit = {
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
extends TreeActivityAgent
{
  import AppState._

  def viewMachine: MVContainer[_]

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

  protected def initialUi: Option[ViewAgent] = None

  override def admit = super.admit orElse {
    case ContentViewReady(ag) if ag == this =>
      _ << MainViewReady.toLocal
    case ActivityAgentStarted(_) =>
      initialUi match {
        case Some(agent) => _ << mvMachine.sendP(LoadUi(agent))
        case _ => s => s
      }
  }
}

trait MVAgent
extends MainViewAgent
{
  lazy val viewMachine = new MVFrame {}
}

case class Drawer(container: DrawerLayout, mainFrame: MainFrame,
  drawer: FrameLayout)
extends ViewTree[DrawerLayout]
with HasMainFrame
{
  container.lp(MATCH_PARENT, MATCH_PARENT)

  drawer.setId(res.R.id.drawer)

  override def toString = className
}

case class ExtMVLayout(container: LinearLayout, toolbar: Toolbar,
  drawer: Drawer)
extends ViewTree[LinearLayout]
with HasMainFrame
{
  container.lp(MATCH_PARENT, MATCH_PARENT)

  override def toString = className

  def mainFrame = drawer.mainFrame
}

trait ExtMVContainer
extends MVContainer[ExtMVLayout]
{
  def infMain = inf[ExtMVLayout]

//   def createDrawerToggle = {
//     drawer.v.map2(toolbar.v) { (d, t) =>
//         act { act =>
//           val res = Resources.fromContext(act)
//           (res.stringId("drawer_open") |@| res.stringId("drawer_close"))
//             .map((o, c) => new ActionBarDrawerToggle(act, d, t, o, c))
//             .map(StoreDrawerToggle(_))
//         }
//     }
//   }
//     StreamIO.lift { implicit a =>
//     drawer >>- { drawer =>
//       toolbar >>- { tb =>
//         res.stringId("drawer_open") |@| res.stringId("drawer_close") map {
//           case (o, c) =>
//             new ActionBarDrawerToggle(activity, drawer, tb, o, c)
//         } toOption
//       }
//     }
//   }
}

trait ExtMV
extends MainViewAgent
{
  lazy val viewMachine = new ExtMVContainer {}
}
