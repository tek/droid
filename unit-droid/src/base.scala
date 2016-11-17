package tryp
package droid
package unit

import state._
import view._

import android.view.Window

import org.robolectric.annotation.Config

abstract class ActivitySpec[A <: Activity]
extends UnitSpecs2Spec[A]
with Matchers
{
  def before = ()
}

class SpecStateActivity
extends StateActivity
{
  override protected def mainViewTimeout = 30 seconds

  override def onCreate(state: Bundle) {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    super.onCreate(state)
  }
}

@Config(application = classOf[SpecStateApplication])
abstract class StateAppSpec
extends ActivitySpec[SpecStateActivity]
{
  def activityClass = classOf[SpecStateActivity]

  def initialAgent: ActivityAgent

  override def before =
    stateApp.publishLocal1(AppState.SetAgent(initialAgent))

  def stateApp = application match {
    case a: SpecStateApplication => a
    case a => sys.error(s"app is not StateApplication: ${a.className}")
  }
}
