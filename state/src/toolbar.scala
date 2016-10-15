package tryp
package droid
package state

import IOOperation.exports._

import android.support.v7.app.ActionBarActivity
import android.view.Gravity

object ToolbarMachineData
{
}

trait ToolbarMachine
extends ViewMachine
{
  import ViewMachine._
  import ToolbarMachineData._
  import view.io.misc._

  // val t = theme.dimension("actionBarSize")
  //   .map(a => T.minHeight(a.toInt)).toOption

  def belowToolbarLayout: StreamIO[_ <: View, Context]

  lazy val toolbar =
    l[Toolbar](
      w[FrameLayout] >>= iota.id[FrameLayout](iota.Id.toolbar)) >>- bgCol("toolbar")// >>-
      // titleColor("toolbar_text") >>-
      // toolbarLp(↔, Height.wrap, Gravity.RIGHT)


  lazy val layout = {
    l[FrameLayout](
      // LL(vertical, llp(↔, ↕))(
      l[LinearLayout](
        toolbar,
        belowToolbarLayout >>= iota.lp(MATCH_PARENT, MATCH_PARENT)
      )
    ) >>- fitsSystemWindows
  }

  override def machinePrefix = super.machinePrefix :+ "toolbar"

  def admit: Admission = {
    case AppState.ContentViewReady(_) =>
      _ << toolbar.v
        .map(t => actAs[ActionBarActivity, Unit](_.setSupportActionBar(t)))
  }
}
