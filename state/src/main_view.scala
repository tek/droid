package tryp
package droid
package state

import shapeless._

import iota._

import tryp.state._
import tryp.state.annotation._

import android.view.ViewGroup.LayoutParams
import android.view.{Gravity, LayoutInflater}
import android.widget._
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle

import view.io.text._
import view.io.misc._
import droid.res.R
import ViewMachineTypes._

case class MainFragment(view: View)
extends Fragment
{
  override def onCreateView
  (inflater: LayoutInflater, container: ViewGroup, state: Bundle) = {
    view
  }
}

class MainFrame(c: Context)
extends FrameLayout(c)
with Logging
{
  setId(System.identityHashCode(this))

  override def addView
  (v: View, index: Int, params: LayoutParams): Unit =
  {
    if (getChildCount != 0)
      log.error(s"Tried to add child $v to MainFrame $toString which already has children")
    else super.addView(v, index, params)
  }

  def setView(a: View) = {
    if (getChildCount > 0) removeViewAt(0)
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
  case class LoadUi(agent: Any)
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

  case class SetMainTree(tree: AnyTree)
  extends Message

  case object Back
  extends Message

  case object UiLoaded
  extends Message

  case object InitUi
  extends Message

  case class ToMainView(m: Message)
  extends Message

  case class ContentTree(tree: AnyTree)
  extends Message
  {
    override def toString = "ContentTree"
  }
}
import MainViewMessages._

@machine
abstract class MVContainer[A <: AnyTree with HasMainFrame: ClassTag]
extends ViewMachineBase[A]
{
  def mvcTrans: Transitions = {
    case CreateContentView =>
      CreateMainView :: ContextIO(infMain.map(ContentTree(_))) :: HNil
    case ContentTree(tree: A) => {
      case s => stateWithTree(s, tree) :: act(_.setContentView(tree.container)).main :: HNil
    }
    case SetMainTree(tree) => {
      case ViewData(main, sub) => setMainView(main, tree.container) :: HNil
    }
    case Back => act(_.onBackPressed()) :: HNil
    case ContentViewReady(agent) => {
      case ViewData(v, _) => (VData(v, Ready): MState) :: InitUi :: HNil
    }
      case UiLoaded => {
      /** If the main frame has already been installed (state Ready), immediately request the main view
      */
      case ViewData(_, Ready) => InitUi :: HNil
    }
  }

  def setMainView(main: A, view: View) = {
    def replaceFragment(v: View)(a: Activity) = {
      a.replaceFragment(main.mainFrame.getId, MainFragment(v), true, "mainframe", false)
      MainViewLoaded
    }
    act(replaceFragment(view)).main
  }
}

trait MVContainerMain
extends AnnotatedTIO
{
  def infMain = inflate[MainViewLayout]
}

@machine
object MVFrame
extends MVContainer[MainViewLayout]
with MVContainerMain

case class Drawer(ctx: Context, container: DrawerLayout, mainFrame: MainFrame, drawer: MainFrame)
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

  override def toString = this.className
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

  override def toString = this.className

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

  case object CloseDrawer
  extends Message
}
import ExtMVContainerData._

@machine
abstract class ExtMVContainerBase[A <: AnyTree with HasDrawer: ClassTag]
extends MVContainer[A]
{
  trait ExtMVDataBase
  extends ViewData
  {
    def toggle: Toggle
  }

  case class ExtMVData(view: A, sub: MState, toggle: Toggle, open: Boolean)
  extends ExtMVDataBase

  def dataWithToggle(main: A, sub: MState, toggle: Toggle): MState =
    ExtMVData(main, sub, toggle, false)

  def toggleTrans: Transitions = {
    case MainViewReady => {
      case ViewData(main, _) =>
        DrawerReady :: CreateDrawerView :: SetupActionBar :: CreateToggle :: HNil
    }
    case SetupActionBar => {
      case ViewData(main, _) =>
        acact { a =>
          a.setSupportActionBar(main.toolbar)
          a.getSupportActionBar.setHomeButtonEnabled(true)
          a.getSupportActionBar.setDisplayHomeAsUpEnabled(true)
        } :: HNil
    }
    case CreateToggle => {
      case ViewData(main, _) =>
        act { a =>
          val t = new Toggle(a, main.drawer.container, main.toolbar, R.string.drawer_open, R.string.drawer_close)
          StoreDrawerToggle(t)
        } :: HNil
    }
    case StoreDrawerToggle(toggle) => {
      case ViewData(main, sub) =>
        dataWithToggle(main, sub, toggle) :: SyncToggle :: HNil
    }
    case DrawerViewReady(v) => {
      case ViewData(main, _) =>
        (main.drawer.drawer >>- MainFrame.load(v)).map(_ => DrawerLoaded) :: HNil
    }
    case SyncToggle => {
      case ExtMVData(_, _, toggle, _) =>
        con(_ => toggle.syncState()) :: HNil
    }
    case CloseDrawer => {
      case ExtMVData(main, _, _, _) =>
        con(_ => main.drawer.container.closeDrawer(Gravity.LEFT)) :: HNil
    }
  }
}
