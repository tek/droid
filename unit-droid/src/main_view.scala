package tryp
package droid
package unit

import android.widget._
import android.view._

import state._

import shapeless._

trait MVViewMachine
extends SimpleViewMachine

class MainViewAA(implicit a: AndroidActivityUiContext,
  res: Resources)
extends droid.state.AppStateActivityAgent
{
  lazy val viewMachine =
    new MVViewMachine {
      lazy val layoutIO = l[FrameLayout](w[EditText] :: HNil)
    }
}
