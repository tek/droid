package tryp
package droid
package test

import scala.reflect.classTag

import android.app.Instrumentation
import android.test.ActivityInstrumentationTestCase2
import android.widget._
import android.support.v7.widget.RecyclerView

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
    pre()
    prefs.clear()
    appPrefs.clear()
    setActivityInitialTouchMode(false)
    solo
    post()
  }

  def pre() { }

  def post() { }

  override def tearDown() {
    solo.finalize()
    activity.finish()
    super.tearDown()
  }

  def stopActivity() {
    activity.finish()
    idleSync()
  }

  def idleSync() {
    activity
    instr.waitForIdleSync()
  }

  def waitFor(timeout: Int)(predicate: => Boolean) {
    solo.waitForCondition(new Condition {
      override def isSatisfied: Boolean = predicate
    }, timeout)
  }

  def assertion(isTrue: => Boolean, msg: => String) = assert(isTrue, msg)

  def enterText(text: String, id: Int = 0) {
    activity.viewOfType[EditText] foreach {
      solo.enterText(_, text)
    }
  }

  implicit class ViewAssertions[A: droid.SearchView](target: A) {
    def recycler = {
      target.viewOfType[RecyclerView] effect { r =>
        idleSync()
        r.measure(0, 0)
        r.layout(0, 0, 100, 10000)
      }
    }

    def nonEmptyRecycler(count: Long) = {
      assertWM {
        recycler
          .map(r => s"recycler childcount ${r.getChildCount} != $count")
          .getOrElse("recycler doesn't exist")
      } { recycler exists(_.getChildCount == count) }
      recycler
    }
  }
}
