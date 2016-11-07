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
    val end = System.currentTimeMillis + timeout
    while(!predicate && System.currentTimeMillis < end) Thread.sleep(100)
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
        r.measure(0, 0)
        r.layout(0, 0, 100, 10000)
      }
    }

    def nonEmptyRecycler(timeout: Double) = {
      assertWMFor(
        recycler
          .map(r => "recycler empty")
          .getOrElse("recycler doesn't exist")
        )((timeout * 1000).toInt)(ui(recycler.exists(_.getChildCount > 0)))
      recycler
    }

    def recyclerWith(count: Long) = {
      assertWM {
        recycler
          .map(r => s"recycler childcount ${r.getChildCount} != $count")
          .getOrElse("recycler doesn't exist")
      } { recycler exists(_.getChildCount == count) }
      recycler
    }
  }

  def ui[A](f: => A) = {
    cio(_ => f).main.unsafePerformSync
  }

  def sleep(secs: Double) = Thread.sleep((secs * 1000).toInt)
}

class StateSpec[A <: StateActivity](cls: Class[A])
extends TrypIntegrationSpec[A](cls)
{
  def stateActivity = activity match {
    case a: StateActivity => a
    case _ => sys.error("activity is not a StateActivity")
  }
}
