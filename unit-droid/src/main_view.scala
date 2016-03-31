package tryp
package droid
package unit

import state.core._
import state._
import view.core._
import view._
import io.text._

import android.widget._
import android.view._
import ViewGroup.LayoutParams._

import shapeless._

import scalaz.stream, stream.{Process, time}, Process._

import iota._, iota.std.TextCombinators._

import MainViewMessages.LoadUi

trait MVViewMachine
extends ViewMachine

class MainView1
extends MainViewAgent
{
  override def handle = "mv1"

  implicit val sched = stream.DefaultScheduler

//   override def initialMessages =
//     time.sleep(2 seconds) ++ emit(LoadUi(new Agent3).publish)
}

class Agent3
extends ViewAgent
{
  def handle = "a3"

  lazy val viewMachine = new ViewMachine {
    lazy val layoutIO = c[FrameLayout](
      w[EditText] >>= large >>= text("view agent 1") >>=
        lpK(WRAP_CONTENT, WRAP_CONTENT) { p: FrameLayout.LayoutParams =>
          p.gravity = Gravity.CENTER
        }
      )
  }
}
