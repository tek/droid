package tryp
package droid
package state

import cats.syntax.all._

import scalaz.stream._
import Process._
import AgentStateData._

import core._
import droid.core._
import view.core._
import view._

object AppState
{
  case class SetActivity(activity: Activity)
  extends Message

  case class StartActivity(agent: ActivityAgent)
  extends Message

  case class SetAgent(agent: ActivityAgent)
  extends Message

  case object Ready
  extends BasicState

  case object AgentInitialized
  extends Message

  case class ActivityAgentStarted(agent: ActivityAgent)
  extends Message

  case class ASData(activity: Option[Activity], agent: Option[ActivityAgent])
  extends Data
}
import AppState._

trait AppStateMachine
extends Machine
{
  override def handle = "app_state"

  def initialAgent: ActivityAgent

  def admit: Admission = {
    case AppState.StartActivity(a) => startActivity(a)
    case SetActivity(a) => setActivity(a)
    case SetAgent(a) => setAgent(a)
    case ActivityAgentStarted(agent) => activityStarted(agent)
    case f @ ContextFun(_) => contextFun(f)
    case f @ ActivityFun(_) => activityFun(f)
    case f @ ResourcesFun(_) => resourcesFun(f)
    case t @ ContextTask(_) => contextTask(t)
    case s @ ContextStream(_) => contextStream(s)
  }

  def startActivity(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(_, _)) =>
      s << ContextFun(_.startActivityCls(agent.activityClass)) <<
        SetAgent(agent)
  }

  def setAgent(agent: ActivityAgent): Transit = {
    case S(Ready, ASData(act, None)) =>
      S(Ready, ASData(act, agent.some)) << AddSub(Nes(agent)).toAgent
    case S(Ready, ASData(act, Some(old))) =>
      S(Ready, ASData(act, agent.some)) << AddSub(Nes(agent)).toAgent <<
        StopSub(Nes(old))
  }

  def setActivity(a: Activity): Transit = {
    case S(Pristine, NoData) =>
      S(Ready, ASData(Some(a), None)) << SetAgent(initialAgent)
    case S(Ready, ASData(_, ag)) =>
      S(Ready, ASData(Some(a), ag))
  }

  def initAgent: Transit = {
    case s @ S(Ready, ASData(_, Some(ag))) =>
      s << ag.publish(Create(Map(), None))
  }

  import IOEffect.ops._

  def activityStarted(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(Some(act), Some(ag))) if agent == ag =>
      s << ag.safeViewIO
        .map(_ >>- (act.setContentView(_: View)))
        .map(_.unitUi)
  }

  def contextFun(task: ContextFun[_]): Transit =
  {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << task.task(act)
  }

  def activityFun(task: ActivityFun[_]): Transit =
  {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << task.task((act))
  }

  def resourcesFun(task: ResourcesFun[_]): Transit =
  {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << task.task((Resources.fromContext(act)))
  }

  def contextTask(task: ContextTask[_]): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << stateEffectTask("context task")(task.task(act))
  }

  def contextStream(task: ContextStream[_]): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << stateEffectProc("context stream")(task.task(act))
  }
}

trait StateApplication
extends ApplicationI
with RootAgent { app: android.app.Application =>

  override def handle = "state_app"

  lazy val appStateMachine = new AppStateMachine {
    def initialAgent = app.initialAgent
  }

  lazy val ioMachine = new IODispatcher {}

  override def machines = ioMachine %:: appStateMachine %:: super.machines

  abstract override def onCreate() {
    forkAgent()
    super.onCreate()
  }

  def setActivity(act: Activity) = {
    scheduleOne(SetActivity(act).toLocal)
  }

  def initialAgent: ActivityAgent
}
