package tryp
package droid
package state

import IOOperation.exports._

import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle

object DrawerMachineData
{
  case class DrawerData(toggle: ActionBarDrawerToggle)
  extends Data

  case class StoreDrawerToggle(toggle: ActionBarDrawerToggle)
  extends Message
}

trait DrawerMachine
extends IOViewMachine[DrawerLayout]
{
  import ViewMachine._
  import DrawerMachineData._

  def contentLayout: StreamIO[_ <: View, Context]

  def toolbar: StreamIO[_ <: Toolbar, Context]

  override def machinePrefix = super.machinePrefix :+ "drawer"

  lazy val layout = l[DrawerLayout](
      contentLayout,
      w[FrameLayout] >>- iota.effect.id[FrameLayout](iota.module.Id.drawer)
    )

  def drawer = layout

  override def extraAdmit = super.extraAdmit orElse {
    case AppState.ContentViewReady(_) => setupToggle
    case StoreDrawerToggle(toggle) => storeToggle(toggle)
  }

  def setupToggle: Transit = {
    case s @ S(_, NoData) =>
      s
      // s << createDrawerToggle
  }

  def storeToggle(toggle: ActionBarDrawerToggle): Transit = {
    case S(s, NoData) =>
      S(s, DrawerData(toggle))
  }

  def createDrawerToggle = {
    drawer.v.map2(toolbar.v) { (d, t) =>
        act { act =>
          val res = Resources.fromContext(act)
          (res.stringId("drawer_open") |@| res.stringId("drawer_close"))
            .map((o, c) => new ActionBarDrawerToggle(act, d, t, o, c))
            .map(StoreDrawerToggle(_))
        }
    }
  }
    // StreamIO.lift { implicit a =>
    // drawer >>- { drawer =>
    //   toolbar >>- { tb =>
    //     res.stringId("drawer_open") |@| res.stringId("drawer_close") map {
    //       case (o, c) =>
    //         new ActionBarDrawerToggle(activity, drawer, tb, o, c)
    //     } toOption
    //   }
    // }
  // }


  // private[this] def assembled = {
  //   adapter.flatMap { a => recycler >>- recyclerAdapter(a) }
  // }

  // def layout = l[FrameLayout](assembled)
}

trait DrawerAgent
extends ActivityAgent
with MainViewAgent { ag =>
  lazy val drawerMachine =
    new DrawerMachine {
      override def handle = "spec"

      def contentLayout = ag.viewMachine.layout

      def toolbar = toolbarMachine.toolbar

      def admit: Admission = PartialFunction.empty
    }

//   lazy val mainViewMachine = new MainViewMachine {}

  lazy val toolbarMachine: ToolbarMachine = new ToolbarMachine {
    def belowToolbarLayout = drawerMachine.layout
  }

  // def viewMachine = toolbarMachine

  // override def machines =
  //   mainViewMachine :: drawerMachine :: super.machines
}
