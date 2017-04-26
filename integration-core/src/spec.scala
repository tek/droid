package tryp
package droid
package integration

import com.github.nscala_time.time.Imports._

import scala.reflect.classTag

import android.app.Instrumentation
import android.test.ActivityInstrumentationTestCase2
import android.widget._
import android.support.v7.widget.RecyclerView

import junit.framework.Assert._

import com.robotium.solo._

import droid.state.{MVData, ViewDataI}

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
with HasSettings
with TestHelpers
with AnnotatedIO
{
  def view = null
  implicit def activity: A = getActivity
  def instr: Instrumentation = getInstrumentation
  lazy val solo: Solo = new Solo(instr, activity)

  def settings = Settings.defaultSettings

  def setPrefs(): Unit = ()

  override def setUp() {
    super.setUp()
    pre()
    settings.user.clear()
    settings.app.clear()
    setPrefs()
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
    con(_ => f).main.unsafeRun()
  }

  def sleep(secs: Double) = Thread.sleep((secs * 1000).toInt)
}

abstract class StateSpec[A <: StateActivity](cls: Class[A])
extends IntegrationSpec[A](cls)
{
  def stateActivity = activity match {
    case a: StateActivity => a
    case _ => sys.error("activity is not a StateActivity")
  }

  def stateApp = stateActivity.stateApp match {
    case a: StateApplication => a
    case a => sys.error(s"application is not an IntApplication: $a")
  }

  lazy val root: StateApplicationAgent = stateApp.root

  lazy val appStateCell = root.appStateCell

  def agent: ActivityAgent

  override def post() = {
    super.post()
    activity
    sleep(1)
    stateApp.agents.unsafeSend(AppState.SetAgent(agent))
  }

  def activityAgent =
    appStateCell.current.get.data match {
      case droid.state.AppState.ASData(_, _, Some(agent)) => agent
    case _ => sys.error("no activity agent running")
    }

  def mainAgent =
    activityAgent match {
      case m: MainViewAgent => m
      case _ => sys.error("activity agent is not a main view agent")
    }

  def mainUi: ViewAgent =
    mainAgent.mvCell.current.get.data match {
      case MVData(ui: ViewAgent) => ui
      case _ => sys.error("main view has no ui")
    }

  def mainTree[A: ClassTag] =
    mainUi.viewCell.current.get.data match {
      case a: ViewDataI[_] =>
        a.view match {
        case tree: A => tree
        case _ => sys.error(s"view tree has wrong class: ${a.view}")
      }
      case _ => sys.error("no view tree in main ui")
    }
}
