package tryp
package droid
package integration

import com.github.nscala_time.time.Imports._

import scala.reflect.classTag

import android.app.Instrumentation
import android.test.ActivityInstrumentationTestCase2
import android.widget._

import junit.framework.Assert._

import com.robotium.solo._

class IntegrationBase[A <: Activity](cls: Class[A])
extends ActivityInstrumentationTestCase2[A](cls)
with FixedPool
with Logging
{
  def name = "spec"

  val threads = 50

  implicit def scheduler = Scheduler.fromFixedDaemonPool(1)

  lazy val startTime = DateTime.now

  val dateFormat = DateTimeFormat.forPattern("hh:mm:ss.SSS")

  override def setUp() = {
    log.info(s"starting at ${startTime.toString(dateFormat)}")
    super.setUp()
  }

  override def tearDown() {
    log.info(s"finished at ${DateTime.now.toString(dateFormat)}")
    super.tearDown()
  }
}

class IntegrationSpec[A <: Activity](cls: Class[A])
extends IntegrationBase[A](cls)
// with HasSettings
with TestHelpers
with AnnotatedIO
{
  def view = null
  implicit def activity: A = getActivity
  def instr: Instrumentation = getInstrumentation
  lazy val solo: Solo = new Solo(instr, activity)

  // def settings = Settings.defaultSettings

  def setPrefs(): Unit = ()

  override def setUp() {
    super.setUp()
    pre()
    // settings.user.clear()
    // settings.app.clear()
    // setPrefs()
    setActivityInitialTouchMode(false)
    solo
    post()
  }

  def pre() {
  }

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
    while(!predicate && System.currentTimeMillis < end) Thread.sleep(200)
  }

  def assertion(isTrue: => Boolean, msg: => String) = assert(isTrue, msg)

  def enterText(text: String, id: Int = 0) {
    activity.viewOfType[EditText] foreach {
      solo.enterText(_, text)
    }
  }

  implicit class ViewAssertions[A: droid.view.RootView](target: A) {
    def recycler = {
      target.viewOfType[RecyclerView] sideEffect { case r =>
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
    conIO(_ => f).main.unsafeRun()
  }

  def sleep(secs: Double) = Thread.sleep((secs * 1000).toInt)
}

abstract class StateSpec[A <: StateActivity](cls: Class[A])
extends IntegrationSpec[A](cls)
{
  def intAppState = activity.stateApp.state match {
    case a: IntAppState => a
    case _ => sys.error("no IntApp")
  }

  def mainLayout = intAppState.mainView.mainView match {
    case Some(a) => a
    case _ => sys.error("no main view")
  }

  def mainFrame = mainLayout.mainFrame

  def showTree(tree: String) = log.info("\n" + tree)

  def showWindow = showTree(activity.showViewTree)

  def send = intAppState.send _
}
