package tryp
package droid
package unit

import android.widget._
import android.view._

import org.robolectric.annotation.Config

import state._

import shapeless._

trait AppStateSpecMachine
extends ViewMachine

class Agent1
extends ActivityAgent
{
  lazy val viewMachine =
    new AppStateSpecMachine {
      lazy val layoutIO = l[FrameLayout](w[EditText])
    }
}

class StateAppUnitActivity
extends StateAppActivity
{
  override protected def mainViewTimeout = 30 seconds

  override def onCreate(state: Bundle) {
    super.onCreate(state)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
  }
}

@Config(application = classOf[MainViewStateApplication])
abstract class StateAppSpec
extends ActivitySpec[StateAppUnitActivity]
{
  def activityClass = classOf[StateAppUnitActivity]

  def stateApp = application match {
    case a: state.StateApplication => a
    case a => sys.error(s"app is not StateApplication: ${a.className}")
  }
}
