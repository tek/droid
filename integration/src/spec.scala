package tryp
package droid
package integration

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

  implicit val sched = state.DefaultScheduler.scheduler

  lazy val startTime = Time.now

  // val dateFormat = DateTimeFormatter.ofPattern("hh:mm:ss.SSS")

  override def setUp() = {
    startTime
    super.setUp()
  }

  override def tearDown() {
    log.info(s"finished after ${Time.hms(Time.now - startTime)}")
    super.tearDown()
  }
}

class IntegrationSpec[A <: Activity](cls: Class[A])
extends IntegrationBase[A](cls)
// with HasSettings
with AnnotatedIO
{
  implicit def activity: A = getActivity

  def instr: Instrumentation = getInstrumentation

  override def setUp() {
    super.setUp()
    setActivityInitialTouchMode(false)
  }

  override def tearDown() {
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
