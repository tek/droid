package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity

import iota.ViewTree

import fs2._

// import tryp.state.AgentStateData._
import IOOperation._
import tryp.state.{StartAgent, StopAgent}

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

  case class SetContentView(view: View, sender: Option[Machine])
  extends Message

  case class SetContentTree(tree: ViewTree[_ <: ViewGroup],
    sender: Option[Machine])
  extends Message

  case class ContentViewReady(agent: ActivityAgent)
  extends Message

  case object Ready
  extends BasicState

  case class ASData(activity: Option[Activity], agent: Option[ActivityAgent])
  extends Data

  trait ActivityLifecycleMessage
  extends Message
  {
    def activity: Activity
  }

  case class OnStart(activity: Activity)
  extends ActivityLifecycleMessage

  case class OnResume(activity: Activity)
  extends ActivityLifecycleMessage
}
import AppState._

// @tryp.state.annotation.machine
case class AppStateTrans(initialAgent: Option[ActivityAgent], mcomm: MComm)
(implicit sender: Sender, strat: Strategy)
extends view.AnnotatedIO
with MachineTransitions
{
  import AppState._

  // def dbInfo: Option[DbInfo]

  def admit: Admission = {
    case AppState.StartActivity(a) => startActivity(a)
    case SetActivity(a) => setActivity(a)
    case SetAgent(a) => setAgent(a)
    case ActivityAgentStarted(agent) => activityStarted(agent)
    case SetContentView(view, sender) => setContentView(view)
    case SetContentTree(view, sender) => setContentView(view.container)
    case f @ ContextFun(_, _) => contextFun(f)
    case f @ ActivityFun(_, _) => activityFun(f)
    case f @ AppCompatActivityFun(_, _) => appCompatActivityFun(f)
    // case t @ DbTask(_) => dbTask(t)
    // case t @ ECTask(_) => ecTask(t)
    case m: ActivityLifecycleMessage => activityLifecycleMessage(m)
  }

  def startActivity(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(_, _)) =>
      s << con(_.startActivityCls(agent.activityClass)) << SetAgent(agent).back
  }

  def setAgent(agent: ActivityAgent): Transit = {
    case S(Ready, ASData(act, None)) =>
      S(Ready, ASData(act, agent.some)) <<
        StartAgent(Stream(agent)).toAgentMachine
    case S(Ready, ASData(act, Some(old))) =>
      S(Ready, ASData(act, agent.some)) <<
        StartAgent(Stream(agent)).toAgentMachine << StopAgent(old)
  }

  private[this] def agentForActivity(a: Activity) =
    a match {
      case sa: StateActivity => sa.agent
      case _ => None
    }

  def setActivity(a: Activity): Transit = {
    case S(Pristine, NoData) =>
      val agent = agentForActivity(a) orElse initialAgent
      S(Ready, ASData(Some(a), None)) << agent.map(SetAgent(_))
    case S(Ready, ASData(_, ag)) =>
      S(Ready, ASData(Some(a), ag)) << agentForActivity(a).map(SetAgent(_))
  }

  def initAgent: Transit = {
    case s @ S(Ready, ASData(_, Some(ag))) =>
      s << Create(Map(), None).toAgent(ag)
  }

  def activityStarted(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(Some(act), Some(ag))) if agent == ag =>
      s << CreateContentView.to(agent)
  }

  def setContentView(view: View): Transit = {
    case s @ S(Ready, ASData(Some(_), Some(ag))) =>
      s << act(_.setContentView(view)).map(_ => ContentViewReady(ag).toSub).ui
  }

  def contextFun(fun: ContextFun[_]): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << fun.task(act).effect(fun.desc)
  }

  def activityFun(fun: ActivityFun[_]): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << fun.task(act).effect(fun.desc)
  }

  def appCompatActivityFun(fun: AppCompatActivityFun[_]): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      act match {
        case aca: AppCompatActivity => s << fun.task(aca).effect(fun.desc)
        case _ => s << LogError(s"executing $fun", s"wrong type $act")
      }
  }

  // def dbTask(task: DbTask[_, _]): Transit = _ << task.effect(dbInfo)

  // def ecTask(task: ECTask[_]): Transit = _ << task.effect(ec)

  def activityLifecycleMessage(m: ActivityLifecycleMessage): Transit = {
    case s @ S(Ready, ASData(Some(act), Some(agent))) if act == m.activity =>
      m.toAgent(agent)
  }
}

case class AppStateMachine(initialAgent: Option[ActivityAgent])
(implicit strat: Strategy)
extends Machine
{
  def transitions(mcomm: MComm) = AppStateTrans(initialAgent, mcomm)
}

case class StateAppAgentTrans(appStateMachine: AppStateMachine)
(implicit sender: Sender)
{
  def admit: Admission = {
    case a @ SetContentView(_, _) => _ << a.to(appStateMachine)
    case a @ SetContentTree(_, _) => _ << a.to(appStateMachine)
  }
}

trait StateApplicationAgent
extends Agent { app =>
  implicit def strat: Strategy

  lazy val appStateMachine = new AppStateMachine(initialAgent)

  lazy val transitions = StateAppAgentTrans(appStateMachine)

  lazy val ioMachine = new IODispatcherMachine {}

  def machines = ioMachine :: appStateMachine :: Nil

  def initialAgent: Option[ActivityAgent] = None

  // def dbInfo: Option[DbInfo] = None

  def setActivity(act: Activity) = {
    log.debug(s"setting activity $act")
    // schedule1(SetActivity(act).toLocal)
    // send(SetActivity(act))
  }

  def onStart(activity: Activity) =
    ()
    // appStateMachine.send(OnStart(activity))
    // send(OnStart(activity))

  def onResume(activity: Activity) =
    ()
    // appStateMachine.send(OnResume(activity))
    // send(OnResume(activity))
}

trait StateApplication
{
  def agents: Agents

  // def stateAppAgent: StateApplicationAgent
}
