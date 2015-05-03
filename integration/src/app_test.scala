package tryp.droid.test

import scala.reflect.classTag

import android.app.Instrumentation
import android.test.ActivityInstrumentationTestCase2
import android.widget._

import junit.framework.Assert._

import com.robotium.solo._

import tryp.droid._

class TrypTest[A <: Activity](cls: Class[A])
extends ActivityInstrumentationTestCase2[A](cls)
with Preferences
with AppPreferences
with BroadcastSend
with HasActivity
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

  def pre() {  }

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

  def wait(predicate: ⇒ Boolean) = waitFor(5000)(predicate)

  def waitFor(timeout: Int)(predicate: ⇒ Boolean) {
    solo.waitForCondition(new Condition {
      override def isSatisfied: Boolean = predicate
    }, timeout)
  }

  def assertW(predicate: ⇒ Boolean) {
    assertWFor(5000)(predicate)
  }

  def assertWFor(timeout: Int)(predicate: ⇒ Boolean) {
    waitFor(timeout)(predicate)
    assert(predicate)
  }

  def enterText(text: String, id: Int = 0) {
    activity.viewOfType[EditText] foreach {
      solo.enterText(_, text)
    }
  }

  def frag[A <: Fragment: ClassTag](names: String*) =
    activity.findNestedFrag[A](names: _*)
}

trait TrypTestImplicits
{
  implicit class `Option with assertion`[A: ClassTag](o: Option[A]) {
    assertTrue(s"Option of type ${classTag[A].className} is empty", o.nonEmpty)

    def foreachA(f: A ⇒ Unit) {
      o foreach f
    }

    def flatMapA[B](f: A ⇒ Option[B]) = {
      o flatMap f
    }

    def mapA[B](f: A ⇒ B) = {
      o map f
    }

    def getA = {
      o getOrElse { sys.error("unreachable") }
    }
  }
}

object TrypTest extends TrypTestImplicits
