package tryp
package droid
package state

import tryp.state._

import scalaz.stream._
import Process._
import AgentStateData._

import droid.core._
import view.core._
import view._
import IOOperation._

object AppState
{
  case class SetActivity(activity: Activity)
  extends Message

  case class StartActivity(agent: ActivityAgent)
  extends Message

  case class SetAgent(agent: ActivityAgent)
  extends Message

  case object AgentInitialized
  extends Message

  case class ActivityAgentStarted(agent: ActivityAgent)
  extends Message

  case object CreateContentView
  extends Message

  case class SetContentView(view: View)
  extends Message

  case class ContentViewReady(agent: ActivityAgent)
  extends Message

  case object Ready
  extends BasicState

  case class ASData(activity: Option[Activity], agent: Option[ActivityAgent])
  extends Data
}
import AppState._

trait AppStateMachine
extends Machine
with view.AnnotatedIO
{
  override def handle = "app_state"

  def initialAgent: Option[ActivityAgent]

  def dbInfo: Option[DbInfo]

  def admit: Admission = {
    case AppState.StartActivity(a) => startActivity(a)
    case SetActivity(a) => setActivity(a)
    case SetAgent(a) => setAgent(a)
    case ActivityAgentStarted(agent) => activityStarted(agent)
    case SetContentView(view) => setContentView(view)
    case f @ ContextFun(_) => contextFun(f)
    case f @ ActivityFun(_) => activityFun(f)
    case t @ DbTask(_) => dbTask(t)
    case t @ ECTask(_) => ecTask(t)
  }

  def startActivity(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(_, _)) =>
      s << con(_.startActivityCls(agent.activityClass)) <<
        SetAgent(agent)
  }

  def setAgent(agent: ActivityAgent): Transit = {
    case S(Ready, ASData(act, None)) =>
      S(Ready, ASData(act, agent.some)) << AddSub(Nel(agent)).toAgent
    case S(Ready, ASData(act, Some(old))) =>
      S(Ready, ASData(act, agent.some)) << AddSub(Nel(agent)).toAgent <<
        StopSub(Nel(old))
  }

  def setActivity(a: Activity): Transit = {
    case S(Pristine, NoData) =>
      S(Ready, ASData(Some(a), None)) << initialAgent.map(SetAgent(_))
    case S(Ready, ASData(_, ag)) =>
      S(Ready, ASData(Some(a), ag))
  }

  def initAgent: Transit = {
    case s @ S(Ready, ASData(_, Some(ag))) =>
      s << ag.publish(Create(Map(), None))
  }

  def activityStarted(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(Some(act), Some(ag))) if agent == ag =>
      s << CreateContentView.toSub
  }

  def setContentView(view: View): Transit = {
    case s @ S(Ready, ASData(Some(_), Some(ag))) =>
      s << act(_.setContentView(view)).map(_ => ContentViewReady(ag).toSub).ui
  }

  def contextFun(fun: ContextFun[_]): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << fun.task(act)
  }

  def activityFun(fun: ActivityFun[_]): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << fun.task(act)
  }

  def dbTask(task: DbTask[_, _]): Transit = _ << task.effect(dbInfo)

  def ecTask(task: ECTask[_]): Transit = _ << task.effect(ec)
}

trait StateApplicationAgent
extends RootAgent
{ app =>
  override def handle = "state_app"

  lazy val appStateMachine = new AppStateMachine {
    def initialAgent = app.initialAgent
    def dbInfo = app.dbInfo
  }

  lazy val ioMachine = new IODispatcher {}

  override def machines = ioMachine :: appStateMachine :: super.machines

  def initialAgent: Option[ActivityAgent] = None

  def dbInfo: Option[DbInfo] = None
}

trait StateApplication
{
  def setActivity(act: Activity)
}
