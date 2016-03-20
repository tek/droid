package tryp
package droid
package unit

import android.widget._
import android.view._

import state._

import shapeless._

trait MVViewMachine
extends ViewMachine

class MainViewAA
extends ASMainView

class MainView2
extends ActAgent
{
  import io.misc._

  lazy val viewMachine = new ViewMachine {
    lazy val content = l[FrameLayout](
      w[EditText] :: HNil
      ) >>- metaName("content frame")

    lazy val layoutIO = {
      l[FrameLayout](content :: HNil) >>- metaName("root frame") >>-
        bgCol("main")
    }
  }
}
