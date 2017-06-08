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

case class MainFragment(view: View)
extends Fragment
{
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, state: Bundle) = view
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

  case object MVCReady
  extends Message

  case object MainViewLoaded
  extends Message

  case object CreateMainView
  extends Message

  case object CreateExtMainView
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

@cell
trait MVContainer
extends ViewCell
{
  type CellTree <: AnyTree with HasMainFrame

  def mvcTrans: Transitions = {
    case CreateContentView => CreateTree :: HNil
    case InsertTree(tree) => insertTree(setContentView(tree))
    case TreeInserted => MVCReady :: HNil
    case SetMainTree(tree) => {
      case ViewData(main, extra) => setMainView(main, tree.container) :: HNil
    }
    case Back => actU(_.onBackPressed()) :: HNil
  }

  def setMainView(main: CellTree, view: View) = {
    mainView = Some(main)
    def replaceFragment(v: View)(a: Activity) = {
      a.replaceFragment(main.mainFrame.getId, MainFragment(v), true, "mainframe", false)
      MainViewLoaded
    }
    act(replaceFragment(view)).main
  }

  var mainView: Option[CellTree] = None

  def setContentView(tree: CellTree) = actU(_.setContentView(tree.container)).main

  def createView = ContextIO(infMain.map(ContentTree(_))) :: HNil
}

trait MVContainerMain
extends AnnotatedTIO
{
  def infMain = inflate[MainViewLayout]
}

@cell
object MVFrame
extends MVContainer
with MVContainerMain
{
  type CellTree = MainViewLayout

  def narrowTree(tree: AnyTree) = tree match {
    case t: MainViewLayout => Some(t)
    case _ => None
  }

  def mvfTrans: Transitions = {
    case MVCReady => CreateMainView :: HNil
  }
}

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

case class ExtMVLayout(container: LinearLayout, toolbar: Toolbar, drawer: Drawer)
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

  case object LoadDrawerLayout
  extends Message

  case class StoreDrawerToggle(toggle: Toggle)
  extends Message

  case object DrawerReady
  extends Message

  case object CreateDrawerView
  extends Message

  case class SetDrawerTree(tree: AnyTree)
  extends Message

  case object DrawerLoaded
  extends Message

  case object CreateToggle
  extends Message

  case object SyncToggle
  extends Message

  case object CloseDrawer
  extends Message
}
import ExtMVContainerData._

@cell
abstract class ExtMVContainer
extends MVContainer
{
  type CellTree <: AnyTree with HasDrawer

  case class ExtMVData(toggle: Toggle, open: Boolean)
  extends CState

  def createToggle(a: Activity, main: CellTree) =
    new Toggle(a, main.drawer.container, main.toolbar, R.string.drawer_open, R.string.drawer_close)

  def setupActionBar(main: CellTree) =
    acactU { a =>
      a.setSupportActionBar(main.toolbar)
      a.getSupportActionBar.setHomeButtonEnabled(true)
      a.getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    }

  def toggleTrans: Transitions = {
    case LoadDrawerLayout => {
      case ViewData(main, _) =>
        setupActionBar(main).main :: CreateToggle :: CreateExtMainView :: CreateDrawerView :: HNil
    }
    case CreateToggle => {
      case ViewData(main, _) =>
        act(a => StoreDrawerToggle(createToggle(a, main))) :: HNil
    }
    case StoreDrawerToggle(toggle) => {
      case s => insertExtra(s, ExtMVData(toggle, false)).map(_ :: SyncToggle.local :: HNil)
    }
    case SetDrawerTree(v) => {
      case ViewData(main, _) =>
        (main.drawer.drawer >>- MainFrame.load(v.container)).map(_ => DrawerLoaded).main :: HNil
    }
    case SyncToggle => {
      case Extra(ExtMVData(toggle, _)) =>
        conIO(_ => toggle.syncState()).unitMain :: HNil
    }
    case CloseDrawer => {
      case ViewData(main, _) =>
        conIO(_ => main.drawer.container.closeDrawer(Gravity.LEFT)).unitMain :: HNil
    }
  }
}

@cell
object ExtMVFrame
extends ExtMVContainer
with AnnotatedTIO
{
  type CellTree = ExtMVLayout

  def infMain = inflate[ExtMVLayout]

  def narrowTree(tree: AnyTree) = tree match {
    case t: ExtMVLayout => Some(t)
    case _ => None
  }

  def emvfTrans: Transitions = {
    case MVCReady => LoadDrawerLayout :: HNil
    case CreateExtMainView => CreateMainView :: HNil
  }
}

@cell
trait MainViewCell
extends ViewCell
{
  def trans: Transitions = {
    case CreateMainView => CreateTree :: HNil
    case InsertTree(tree) => insertTree(SetMainTree(tree))
  }
}

@cell
trait DrawerViewCell
extends ViewCell
{
  def trans: Transitions = {
    case CreateDrawerView => CreateTree :: HNil
    case InsertTree(tree) => insertTree(SetDrawerTree(tree))
  }
}
