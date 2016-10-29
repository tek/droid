package tryp
package droid
package integration

import scala.reflect.classTag

import android.app.Instrumentation
import android.test.ActivityInstrumentationTestCase2
import android.widget._
import android.support.v7.widget.RecyclerView

import junit.framework.Assert._

import com.robotium.solo._

class TrypIntegrationSpec[A <: Activity](cls: Class[A])
extends ActivityInstrumentationTestCase2[A](cls)
with HasSettings
with TestHelpers
{
  // def frag[A <: Fragment: ClassTag](names: String*) =
  //   activity.findNestedFrag[A](names)

  def view = null
  implicit def activity: A = getActivity
  def instr: Instrumentation = getInstrumentation
  lazy val solo: Solo = new Solo(instr, activity)

  def settings = Settings.defaultSettings

  override def setUp() {
    super.setUp()
    pre()
    settings.user.clear()
    settings.app.clear()
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

  implicit class ViewAssertions[A: droid.view.RootView](target: A) {
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
