package tryp
package droid
package integration

import android.widget._
import android.view.Gravity
import android.view.ViewGroup.LayoutParams._

import shapeless._

import iota._

import MainViewMessages.LoadUi
import state.AppState._
import IOOperation._
import state.TreeViewTrans

class Simple
extends state.MVAgent
{
  lazy val tMachine = new Machine {
    def name = "t"

    def transitions(mc: MComm) =
      new MachineTransitions {
        def mcomm = mc

        def admit: Admission = {
          case ActivityAgentStarted(_) => _ << LoadUi(new ViewAgent1).broadcast
        }
      }
  }

  override def machines = tMachine :: super.machines

  def agents = Nil
}

case class Main1(container: FrameLayout, label: TextView)
extends ViewTree[FrameLayout]
{
  label.gravity(Gravity.CENTER)
  label.setText("LABEL")

  override def toString = "Main1"
}

class ViewAgent1
extends ViewAgent
{
  def name = "view_1"

  def agents = Nil

  lazy val viewMachine =
    new state.ViewMachine {
      def transitions(mc: MComm) =
        new state.TreeViewTrans[Main1] {
          def mcomm = mc
          def admit = PartialFunction.empty
          def infMain = inf[Main1]
        }
    }
}
