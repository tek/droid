package tryp
package droid
package trial

import android.widget._
import android.view.Gravity
import android.view.ViewGroup.LayoutParams._

import shapeless._

import iota._, iota.std.TextCombinators._

import scalaz.stream.Process._

import view._
import state._
import io.misc._
import io.text._
import MainViewMessages.LoadUi

class TAgent
extends ActivityAgent
{
  lazy val viewMachine = new ViewMachine {
    lazy val but = w[Button] >>= large >>= text("agent 2") >>=
      hook.onClick { (v: View) =>
        IO {
          broadcast(AppState.StartActivity(new TAgent2))
        }
      }

    lazy val layoutIO = {
      l[FrameLayout](but :: HNil) >>- metaName("root frame") >>- bgCol("main")
    }
  }
}

class TMainViewAgent
extends MainViewAgent
{
  override def handle = "t_main_view"

  override def initialMessages = emit(LoadUi(new ViewAgent1).publish)
}

class ViewAgent1
extends ViewAgent
{
  override def handle = "view_1"

  lazy val viewMachine = new ViewMachine {
    lazy val layoutIO = c[FrameLayout](
      w[TextView] >>= large >>= text("view agent 1") >>=
        lpK(WRAP_CONTENT, WRAP_CONTENT) { p: FrameLayout.LayoutParams =>
          p.gravity = Gravity.CENTER
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
          broadcast(AppState.StartActivity(new TAgent))
        }
      }

    lazy val layoutIO = {
      l[FrameLayout](but :: HNil) >>- metaName("root frame") >>- bgCol("main")
    }
  }
}
