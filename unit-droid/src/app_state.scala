package tryp
package droid
package unit

import android.widget._
import android.view._

import state._

import shapeless._

trait AppStateSpecMachine
extends SimpleViewMachine

class AppStateActivityAgent(implicit a: AndroidActivityUiContext,
  res: Resources)
extends droid.state.AppStateActivityAgent
{
  lazy val viewMachine =
    new AppStateSpecMachine {
      lazy val layoutIO = l[FrameLayout](w[EditText] :: HNil)
    }
}

class UnitActivity1
extends StateAppViewActivity
{
  override def onCreate(state: Bundle) {
    super.onCreate(state)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
  }
}
