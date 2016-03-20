package tryp
package droid
package unit

import android.widget._
import android.view._

import state._

import shapeless._

trait AppStateSpecMachine
extends ViewMachine

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
  override protected def mainViewTimeout = 30 seconds

  override def onCreate(state: Bundle) {
    super.onCreate(state)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
  }
}

trait StateAppSpec[A <: Activity]
extends ActivitySpec[A]
{
  def stateApp = application match {
    case a: state.StateApplication => a
    case a => sys.error(s"app is not StateApplication: ${a.className}")
  }
}
