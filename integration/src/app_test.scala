package tryp
package test

import scala.reflect.classTag

import android.app.Instrumentation
import android.test.ActivityInstrumentationTestCase2
import android.widget._

import junit.framework.Assert._

import com.robotium.solo._

class TrypIntegrationSpec[A <: TrypActivity](cls: Class[A])
extends ActivityInstrumentationTestCase2[A](cls)
with Preferences
with AppPreferences
with BroadcastSend
with HasActivity
with TrypDroidSpec
{
  def view = null
  implicit def activity: A = getActivity
  def instr: Instrumentation = getInstrumentation
  lazy val solo: Solo = new Solo(instr, activity)

  override def setUp() {
    super.setUp()
    prefs.clear()
    appPrefs.clear()
    pre()
    setActivityInitialTouchMode(false)
    solo
  }

  def pre() { }

  override def tearDown {
    solo.finalize
    activity.finish
    super.tearDown
  }

  def stopActivity {
    activity.finish
    idleSync
    setActivity(null)
  }

  def idleSync {
    activity
    instr.waitForIdleSync
  }

  def waitFor(timeout: Int)(predicate: ⇒ Boolean) {
    solo.waitForCondition(new Condition {
      override def isSatisfied: Boolean = predicate
    }, timeout)
  }

  def assertion(isTrue: Boolean, msg: ⇒ String) = assert(isTrue, msg)

  def enterText(text: String, id: Int = 0) {
    activity.viewOfType[EditText] foreach {
      solo.enterText(_, text)
    }
  }
}
