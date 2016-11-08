package tryp
package droid
package integration

import android.widget._
import android.view.Gravity
import android.view.ViewGroup.LayoutParams._

import shapeless._

import iota._

import scalaz.stream.Process._

import MainViewMessages.LoadUi
import state.AppState._
import IOOperation._
import state.TreeViewMachine


class IntMainViewAgent
extends state.DrawerAgent
{
  lazy val tMachine = new Machine {
    override def handle = "t"

    def admit: Admission = {
      case ActivityAgentStarted(_) => _ << LoadUi(new ViewAgent1).publish
    }
  }

  override def handle = "t_main_view"

  override def machines = tMachine :: super.machines
}

case class Main1(container: FrameLayout, label: TextView)
extends ViewTree[FrameLayout]
{
  label.gravity(Gravity.CENTER)
  label.setText("LABEL")
}

class ViewAgent1
extends ViewAgent
{
  override def handle = "view_1"

  lazy val viewMachine = new TreeViewMachine {
    def admit = PartialFunction.empty

    def infMain = inf[Main1]
  }
}
