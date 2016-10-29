package tryp
package droid
package integration

import android.widget._
import android.view.Gravity
import android.view.ViewGroup.LayoutParams._

import shapeless._

import iota._

import scalaz.stream.Process._

import view.io.misc._
import view.io.text._
import MainViewMessages.LoadUi
import state.AppState.{ActivityAgentStarted, StartActivity, ContentViewReady}
import IOOperation._


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

class ViewAgent1
extends ViewAgent
{
  override def handle = "view_1"

  lazy val viewMachine = new ViewMachine {
    def admit = PartialFunction.empty

    lazy val layout = l[LinearLayout](
      w[Button] >>- large >>- text("start TAgent1") >>=
        lpK(WRAP_CONTENT, WRAP_CONTENT) { p: LinearLayout.LayoutParams =>
          p.gravity = Gravity.CENTER
        } >>= hook.onClick { (v: View) =>
          iota.IO {
            broadcast(StartActivity(new TAgent1))
          }
        },
      w[Button] >>- large >>- text("load ViewAgent2") >>=
        lpK(WRAP_CONTENT, WRAP_CONTENT) { p: LinearLayout.LayoutParams =>
          p.gravity = Gravity.CENTER
        } >>= hook.onClick { (v: View) =>
          iota.IO {
            broadcast(LoadUi(new ViewAgent2))
          }
        }
    )
  }
}

class ViewAgent2
extends ViewAgent
{
  override def handle = "view_1"

  lazy val viewMachine = new RecyclerViewMachine[StringRecyclerAdapter] {
    lazy val adapter = conS(implicit c => new StringRecyclerAdapter {})

    def admit = {
      case ContentViewReady(_) =>
            _ << adapter.v.map(_.updateItems(List("first", "second")).ui)
    }

    override lazy val layout = l[LinearLayout](
      w[Button] >>- large >>- text("start TAgent1") >>=
        lpK(WRAP_CONTENT, WRAP_CONTENT) { p: LinearLayout.LayoutParams =>
          p.gravity = Gravity.CENTER
        } >>= hook.onClick { (v: View) =>
          iota.IO {
            broadcast(StartActivity(new TAgent1))
          }
        },
      w[Button] >>- large >>- text("load ViewAgent1") >>=
        lpK(WRAP_CONTENT, WRAP_CONTENT) { p: LinearLayout.LayoutParams =>
          p.gravity = Gravity.CENTER
        } >>= hook.onClick { (v: View) =>
          iota.IO {
            broadcast(LoadUi(new ViewAgent1))
          }
        }
    )
  }
}

class TAgent1
extends ActivityAgent
{
  lazy val viewMachine = new ViewMachine {
    def admit = PartialFunction.empty

    lazy val but = w[Button] >>- large >>- text("agent 2") >>=
      hook.onClick { (v: View) =>
        iota.IO {
          broadcast(StartActivity(new TAgent2))
        }
      }

    lazy val layout = {
      l[FrameLayout](but) >>- metaName("root frame") >>- bgCol("main")
    }
  }
}

class TAgent2
extends ActivityAgent
{
  lazy val viewMachine = new ViewMachine {
    def admit = PartialFunction.empty

    lazy val but = w[Button] >>- large >>- text("agent 1") >>=
      hook.onClick { (v: View) =>
        iota.IO {
          broadcast(StartActivity(new TAgent1))
        }
      }

    lazy val layout = {
      l[FrameLayout](but) >>- metaName("root frame") >>- bgCol("main")
    }
  }
}
