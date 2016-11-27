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

class Ext
extends state.ExtMV

case class Main1(container: FrameLayout, label: TextView)
extends ViewTree[FrameLayout]
{
  label.gravity(Gravity.CENTER)
  label.setText("LABEL")

  override def toString = "Main1"
}

object ViewAgent1
extends ViewAgent
{
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
