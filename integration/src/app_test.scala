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
with TrypSpec
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

  def waitFor(timeout: Long)(predicate: ⇒ Boolean) {
    solo.waitForCondition(new Condition {
      override def isSatisfied: Boolean = predicate
    }, timeout)
  }

  def timeoutAssertion(isTrue: Boolean) = assert(isTrue)

  def enterText(text: String, id: Int = 0) {
    activity.viewOfType[EditText] foreach {
      solo.enterText(_, text)
    }
  }
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
