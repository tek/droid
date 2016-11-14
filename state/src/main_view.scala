package tryp
package droid
package state

import iota._

import tryp.state._

import android.view.ViewGroup.LayoutParams
import android.view.Gravity
import android.widget._

import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle

import view.io.text._
import view.io.misc._
import AppState._
import IOOperation.exports._

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

object MainFrame
extends Combinators[MainFrame]
{
  def load(v: View) = k(_.setView(v))
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

abstract class MVContainer
[A <: ViewTree[_ <: ViewGroup] with HasMainFrame: ClassTag]
extends TreeViewMachine[A]
{
  override def internalAdmit = super.internalAdmit orElse {
    case SetContentView(view, _) => setMainView(view)
    case SetContentTree(tree, _) => setMainView(tree.container)
    case Back => back
    case ContentViewReady(agent) => contentViewReady
    case UiLoaded => uiLoaded
  }

  /** If the main frame has already been installed (state Ready), immediately
   *  request the main view
   */
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
      s << (main.mainFrame >>- MainFrame.load(view))
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
  lazy val viewMachine = new MVFrame {
    def admit = PartialFunction.empty
  }
}

case class Drawer(ctx: Context, container: DrawerLayout, mainFrame: MainFrame,
  drawer: MainFrame)
extends ViewTree[DrawerLayout]
with HasMainFrame
{
  implicit val c = ctx
  container.lp(MATCH_PARENT, MATCH_PARENT)
  container.desc("drawer root")
  mainFrame.desc("drawer main")
  drawer.setId(res.R.id.drawer)
  val lp = new DrawerLayout.LayoutParams(200.dp, MATCH_PARENT)
  lp.gravity = Gravity.START
  drawer.setLayoutParams(lp)
  drawer.desc("drawer")

  override def toString = className
}

trait HasDrawer
extends HasMainFrame
{
  def drawer: Drawer
  def toolbar: Toolbar
}

case class ExtMVLayout(container: LinearLayout, toolbar: Toolbar,
  drawer: Drawer)
extends ViewTree[LinearLayout]
with HasDrawer
{
  container.lp(MATCH_PARENT, MATCH_PARENT)
  container.setOrientation(LinearLayout.VERTICAL)
  container.desc("ext mv")

  override def toString = className

  def mainFrame = drawer.mainFrame
}

object ExtMVContainerData
{
  type Toggle = ActionBarDrawerToggle

  case class StoreDrawerToggle(toggle: Toggle)
  extends Message

  case object DrawerReady
  extends Message

  case object CreateDrawerView
  extends Message

  case class DrawerViewReady(view: View)
  extends Message

  case object DrawerLoaded
  extends Message

  case object SetupActionBar
  extends Message

  case object CreateToggle
  extends Message

  case object SyncToggle
  extends Message
}
import ExtMVContainerData._

abstract class ExtMVContainerBase
[A <: ViewTree[_ <: ViewGroup] with HasDrawer: ClassTag]
extends MVContainer[A]
{
  trait ExtMVDataBase
  extends ViewData
  {
    def toggle: Toggle
  }

  case class ExtMVData(view: A, toggle: Toggle, open: Boolean)
  extends ExtMVDataBase

  protected def dataWithToggle(main: A, toggle: Toggle): Data =
    ExtMVData(main, toggle, false)

  protected def toggleAdmit: Admission = {
    case MainViewReady => {
      case s @ S(_, ViewData(main)) =>
        s << DrawerReady.toRoot << CreateDrawerView.toRoot << SetupActionBar <<
          CreateToggle
    }
    case SetupActionBar => {
      case s @ S(_, ViewData(main)) =>
        val action = acact { a =>
          a.setSupportActionBar(main.toolbar)
          a.getSupportActionBar.setHomeButtonEnabled(true)
          a.getSupportActionBar.setDisplayHomeAsUpEnabled(true)
        }
        s << action.unitUi
    }
    case CreateToggle => {
      case s @ S(_, ViewData(main)) =>
        val createToggle = act(a => new Toggle(
          a, main.drawer.container, main.toolbar,
          droid.res.R.string.drawer_open, droid.res.R.string.drawer_close)
        )
        s << createToggle.map(StoreDrawerToggle(_).back)
    }
    case StoreDrawerToggle(toggle) => {
      case S(s, ViewData(main)) =>
        S(s, dataWithToggle(main, toggle)) << SyncToggle
    }
    case DrawerViewReady(v) => {
      case s @ S(_, ViewData(main)) =>
        val io = (main.drawer.drawer >>- MainFrame.load(v))
        s << io.map(_ => DrawerLoaded.toRoot).ui
    }
    case SyncToggle => {
      case s @ S(_, ExtMVData(_, toggle, _)) =>
        s << cio(_ => toggle.syncState()).unitUi
    }
  }

  override def internalAdmit = toggleAdmit orElse super.internalAdmit
}

trait ExtMVContainer
extends ExtMVContainerBase[ExtMVLayout]
{
  def infMain = inf[ExtMVLayout]
}

trait ExtMV
extends MainViewAgent
{
  lazy val viewMachine = new ExtMVContainer {
    def admit = PartialFunction.empty
  }
}
