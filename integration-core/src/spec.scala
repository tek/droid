package tryp
package droid
package integration

import android.app.Instrumentation
import android.test.ActivityInstrumentationTestCase2
import android.widget._

import org.specs2.SpecificationLike

import tryp.unit.IOSpec

trait IntegrationBase
extends FixedPool
with Logging
{
  def name = "spec"

  val threads = 50

  implicit val sched = state.DefaultScheduler.scheduler

  lazy val startTime = Time.now

  // val dateFormat = DateTimeFormatter.ofPattern("hh:mm:ss.SSS")
}

trait IntegrationSpec[A <: Activity]
extends IntegrationBase
with AnnotatedAIO
{
  implicit def activity: A

  def instr: Instrumentation

  def setup(): Unit = {
    startTime
  }

  def teardown() = {
    activity.finish()
    log.info(s"finished after ${Time.hms(Time.now - startTime)}")
  }

  def stopActivity() {
    activity.finish()
    idleSync()
  }

  def idleSync() = {
    activity
    instr.waitForIdleSync()
  }

  def ui[A](f: => A) = conIO(_ => f).performMain.unsafeRunSync()

  def sleep(secs: Double) = Thread.sleep((secs * 1000).toInt)
}

trait StateSpec[A <: StateActivity]
extends IntegrationSpec[A]
{
  def appState = activity.stateApp.state match {
    case a: AppState => a
    case _ => sys.error("no IntApp")
  }

  def mainLayout = appState.mainView.mainView match {
    case Some(a) => a
    case _ => sys.error("no main view")
  }

  def mainFrame = mainLayout.mainFrame

  def showTree(tree: String) = log.info("\n" + tree)

  def showWindow = showTree(activity.showViewTree)

  def send = appState.send _

  def unsafeSend = appState.unsafeSend _
}

class InstrumentationSpec[A <: Activity](cls: Class[A])
extends ActivityInstrumentationTestCase2[A](cls)
with IntegrationSpec[A]
{
  implicit def activity = getActivity

  def instr: Instrumentation = getInstrumentation

  override def setUp() = {
    super.setUp()
    setup()
    setActivityInitialTouchMode(false)
  }

  override def tearDown() {
    teardown()
    super.tearDown()
  }
}

class StateInstrumentationSpec[A <: StateActivity](cls: Class[A])
extends InstrumentationSpec[A](cls)
with StateSpec[A]

abstract class SpecsSpec[A <: StateActivity](cls: Class[A])
extends StateInstrumentationSpec[A](cls)
with SpecsBase
with SpecificationLike
with ViewMatchers
with BiMustExpectations

abstract class SpecsIOSpec[A <: StateActivity](cls: Class[A])
extends StateInstrumentationSpec[A](cls)
with SpecsBase
with IOSpec
