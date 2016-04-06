package tryp
package droid
package unit

import state._
import view._

import android.view.Window

import org.robolectric.annotation.Config

abstract class TestViewActivity
extends Activity
with ViewActivity

abstract class ActivitySpec[A <: Activity]
extends UnitSpecs2Spec[A]
with Matchers
{
  def before = ()
}

class StateUnitActivity
extends StateActivity
{
  override protected def mainViewTimeout = 30 seconds

  override def onCreate(state: Bundle) {
    super.onCreate(state)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
  }
}

@Config(application = classOf[SpecStateApplication])
abstract class StateAppSpec
extends ActivitySpec[StateUnitActivity]
{
  def activityClass = classOf[StateUnitActivity]

  def initialAgent: ActivityAgent

  override def before = 
    stateApp.publishLocalOne(AppState.SetAgent(initialAgent))

  def stateApp = application match {
    case a: state.StateApplication => a
    case a => sys.error(s"app is not StateApplication: ${a.className}")
  }
}
