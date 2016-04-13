package tryp
package droid
package trial

import android.widget._
import android.view.Gravity
import android.view.ViewGroup.LayoutParams._

import shapeless._

import iota._, iota.std.TextCombinators._

import scalaz.stream.Process._

import view.{StartActivity =>_, _}
import state._
import io.misc._
import io.text._
import MainViewMessages.LoadUi
import AppState._

class TAgent
extends ActivityAgent
{
  lazy val viewMachine = new ViewMachine {
    lazy val but = w[Button] >>= large >>= text("agent 2") >>=
      hook.onClick { (v: View) =>
        IO {
          broadcast(StartActivity(new TAgent2))
        }
      }

    lazy val layout = {
      l[FrameLayout](but) >>- metaName("root frame") >>- bgCol("main")
    }
  }
}

class TMainViewAgent
extends MainViewAgent
{
  lazy val tMachine = new Machine {
    override def handle = "t"

    def admit: Admission = {
      case ActivityAgentStarted(_) => _ << LoadUi(new ViewAgent1).publish
    }
  }

  override def handle = "t_main_view"

  override def machines = tMachine %:: super.machines
}

class ViewAgent1
extends ViewAgent
{
  override def handle = "view_1"

  lazy val viewMachine = new ViewMachine {
    lazy val layout = c[FrameLayout](
      w[Button] >>= large >>= text("view agent 1") >>=
        lpK(WRAP_CONTENT, WRAP_CONTENT) { p: FrameLayout.LayoutParams =>
          p.gravity = Gravity.CENTER
        } >>= hook.onClick { (v: View) =>
          IO {
            broadcast(StartActivity(new TAgent2))
          }
        }
      )
  }
}

class TAgent2
extends ActivityAgent
{
  lazy val viewMachine = new ViewMachine {
    lazy val but = w[Button] >>= large >>= text("agent 1") >>=
      hook.onClick { (v: View) =>
        IO {
          broadcast(StartActivity(new TAgent))
        }
      }

    lazy val layout = {
      l[FrameLayout](but) >>- metaName("root frame") >>- bgCol("main")
    }
  }
}
