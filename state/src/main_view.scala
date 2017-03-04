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
// import AppState._
// import IOOperation.exports._

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
      log.error(s"Tried to add child $v to MainFrame $toString" +
        " which already has children")
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

  case class SetMainTree[A <: ViewGroup](tree: ViewTree[A])
  extends Message

  case object Back
  extends Message

  case object UiLoaded
  extends Message

  case object InitUi
  extends Message

  case class ToMainView(m: Message)
  extends Message
}
import MainViewMessages._

@machine
abstract class MVContainer
[A <: ViewTree[_ <: ViewGroup] with HasMainFrame: ClassTag]
extends ViewMachine[A]
with AnnotatedIO
{
  def mvcTrans: Transitions = {
    // case SetContentView(view, _) => setMainView(view)
    case SetContentTree(tree, _) => {
      case ViewData(main, sub) => setMainView(main, tree.container) :: HNil
    }
    case Back => act(_.onBackPressed()) :: HNil
    case ContentViewReady(agent) => {
      case ViewData(v, _) =>
        (VData(v, Ready): MState) :: InitUi :: HNil
        // InitUi.toLocal
    }
      case UiLoaded => {
      /** If the main frame has already been installed (state Ready), immediately
      *  request the main view
      */
      case ViewData(_, Ready) => InitUi :: HNil
        // InitUi.toLocal
    }
  }

  def setMainView(main: A, view: View) = {
    def replaceFragment(v: View)(a: Activity) = {
      a.replaceFragment(main.mainFrame.getId, MainFragment(v), true, "mainframe", false)
      MainViewLoaded
    }
    act(replaceFragment(view)).ui
  }
}

trait MVContainerMain
extends Views[Context, IO]
{
  def infMain = inf[MainViewLayout]
}

@machine
object MVFrame
extends MVContainer[MainViewLayout]
with MVContainerMain

// case class MVData(ui: Agent)
// extends MState

// @machine
// trait MV
// extends IOTrans
// {
//   def mvTrans: Transitions = {
//     case LoadUi(ui) => loadUi(ui)
//     case InitUi => initUi
//   }

//   private[this] def integrate = (ag: Agent) => {
//     (comm: Comm) => ag.integrate(comm).map {
//       case UnwrapMessage(m: SetContentTree) => ToMainView(m)
//       case a => a
//     }
//   }

//   def loadUi(ui: ViewAgent): Transit = {
//     case S(s, _) =>
//       S(s, MVData(ui)) << StartAgentExt(Stream(ui), integrate).broadcast <<
//         UiLoaded.toLocal
//   }

//   def initUi: Transit = {
//     case s @ S(_, MVData(agent)) =>
//       s << CreateContentView.to(agent)
//   }
// }

// class MVATrans(viewMachine: ViewMachine, mvMachine: MV.MV,
//   initialUi: Option[ViewAgent])
// extends ViewAgentTrans
// {
//   def setContentViewAdmit: Transitions = {
//     case m @ SetContentView(view, Some(sender)) if sender != viewMachine =>
//       _ << m.to(viewMachine).publish
//     case m @ SetContentView(_, _) =>
//       _ << ToAppState(m).publish
//     case m @ SetContentTree(view, Some(sender)) if sender != viewMachine =>
//       _ << m.to(viewMachine).publish
//     case m @ SetContentTree(_, _) =>
//       _ << ToAppState(m).publish
//   }

//   def internalAdmit: Transitions = {
//     case ContentViewReady(ag) if mcomm.agent.contains(ag) =>
//       _ << MainViewReady.broadcast
//     case ActivityAgentStarted(_) =>
//       initialUi match {
//         case Some(agent) => _ << LoadUi(agent).to(mvMachine)
//         case _ => s => s
//       }
//   }
// }

// // trait MainViewAgent
// // extends TreeActivityAgent
// // {
// //   import AppState._

// //   def viewMachine: MVContainerMachine[_]

// //   override def transitions(comm: MComm) =
// //     MVATrans(comm, viewMachine, mvMachine, initialUi)

// //   lazy val mvMachine = MV.MV()

// //   override def machines: List[Machine] = mvMachine :: super.machines

// //   protected def initialUi: Option[ViewAgent] = None

// //   override def transformAgentIn = {
// //     case ToMainView(m) => m.to(viewMachine)
// //     case a => super.transformAgentIn(a)
// //   }
// // }

// // trait MVAgent
// // extends MainViewAgent
// // {
// //   lazy val viewMachine = new MVFrameMachine {}
// // }

// case class Drawer(ctx: Context, container: DrawerLayout, mainFrame: MainFrame,
//   drawer: MainFrame)
// extends ViewTree[DrawerLayout]
// with HasMainFrame
// {
//   implicit val c = ctx
//   container.lp(MATCH_PARENT, MATCH_PARENT)
//   container.desc("drawer root")
//   mainFrame.desc("drawer main")
//   drawer.setId(res.R.id.drawer)
//   val lp = new DrawerLayout.LayoutParams(200.dp, MATCH_PARENT)
//   lp.gravity = Gravity.START
//   drawer.setLayoutParams(lp)
//   drawer.desc("drawer")

//   override def toString = this.className
// }

// trait HasDrawer
// extends HasMainFrame
// {
//   def drawer: Drawer
//   def toolbar: Toolbar
// }

// case class ExtMVLayout(container: LinearLayout, toolbar: Toolbar,
//   drawer: Drawer)
// extends ViewTree[LinearLayout]
// with HasDrawer
// {
//   container.lp(MATCH_PARENT, MATCH_PARENT)
//   container.setOrientation(LinearLayout.VERTICAL)
//   container.desc("ext mv")

//   override def toString = this.className

//   def mainFrame = drawer.mainFrame
// }

// object ExtMVContainerData
// {
//   type Toggle = ActionBarDrawerToggle

//   case class StoreDrawerToggle(toggle: Toggle)
//   extends Message

//   case object DrawerReady
//   extends Message

//   case object CreateDrawerView
//   extends Message

//   case class DrawerViewReady(view: View)
//   extends Message

//   case object DrawerLoaded
//   extends Message

//   case object SetupActionBar
//   extends Message

//   case object CreateToggle
//   extends Message

//   case object SyncToggle
//   extends Message

//   case object CloseDrawer
//   extends Message
// }
// import ExtMVContainerData._

// @machine
// abstract class ExtMVContainerBase
// [A <: ViewTree[_ <: ViewGroup] with HasDrawer: ClassTag]
// extends MVContainer[A]
// {
//   trait ExtMVDataBase
//   extends ViewData
//   {
//     def toggle: Toggle
//   }

//   case class ExtMVData(view: A, toggle: Toggle, open: Boolean)
//   extends ExtMVDataBase

//   protected def dataWithToggle(main: A, toggle: Toggle): Data =
//     ExtMVData(main, toggle, false)

//   def toggleTrans: Transitions = {
//     case MainViewReady => {
//       case s @ S(_, ViewData(main)) =>
//         DrawerReady.broadcast << CreateDrawerView.broadcast <<
//           SetupActionBar << CreateToggle
//     }
//     case SetupActionBar => {
//       case s @ S(_, ViewData(main)) =>
//         val action = acact { a =>
//           a.setSupportActionBar(main.toolbar)
//           a.getSupportActionBar.setHomeButtonEnabled(true)
//           a.getSupportActionBar.setDisplayHomeAsUpEnabled(true)
//         }
//         s << action.unitUi
//     }
//     case CreateToggle => {
//       case s @ S(_, ViewData(main)) =>
//         val createToggle = act(a => new Toggle(
//           a, main.drawer.container, main.toolbar,
//           droid.res.R.string.drawer_open, droid.res.R.string.drawer_close)
//         )
//         s << createToggle.map(StoreDrawerToggle(_).back)
//     }
//     case StoreDrawerToggle(toggle) => {
//       case S(s, ViewData(main)) =>
//         S(s, dataWithToggle(main, toggle)) << SyncToggle
//     }
//     case DrawerViewReady(v) => {
//       case s @ S(_, ViewData(main)) =>
//         val io = (main.drawer.drawer >>- MainFrame.load(v))
//         s << io.map(_ => DrawerLoaded.broadcast).ui
//     }
//     case SyncToggle => {
//       case s @ S(_, ExtMVData(_, toggle, _)) =>
//         con(_ => toggle.syncState()).unitUi
//     }
//     case CloseDrawer => {
//       case s @ S(_, ExtMVData(main, _, _)) =>
//         con(_ => main.drawer.container.closeDrawer(Gravity.LEFT)).unitUi
//     }
//   }
// }

// // trait ExtMV
// // extends MainViewAgent
// // {
// //   lazy val viewMachine = new MVContainerMachine[ExtMVLayout] {
// //     def transitions(mc: MComm) = new ExtMVContainer {
// //       val mcomm = mc
// //       def admit = PartialFunction.empty
// //     }
// //   }
// // }
