package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity
import android.app.Application

import iota.ViewTree

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

  case class ASData(app: Application, activity: Option[Activity],
    agent: Option[ActivityAgent])
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

  case class ToAppState(message: Message)
  extends Message

  case object InitApp
  extends Message

  case class SetApplication(app: Application)
  extends Message
}
import AppState._

// @tryp.state.annotation.machine
case class AppStateTrans(initialAgent: Option[ActivityAgent], mcomm: MComm)
extends view.AnnotatedIO
with MachineTransitions
{
  // def dbInfo: Option[DbInfo]

  def admit: Admission = {
    case InitApp => init
    case SetApplication(app) => setApplication(app)
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

  def init: Transit = {
    case s =>
      android.os.Process.setThreadPriority(-15)
      s
  }

  def setApplication(app: Application): Transit = {
    case S(s, ASData(_, act, ag)) =>
      S(Ready, ASData(app, act, ag))
    case S(s, _) =>
      S(Ready, ASData(app, None, None))
  }

  def startActivity(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(_, _, _)) =>
      s << con(_.startActivityCls(agent.activityClass)) << SetAgent(agent).back
  }

  def setAgent(agent: ActivityAgent): Transit = {
    case S(Ready, ASData(app, act, None)) =>
      S(Ready, ASData(app, act, agent.some)) << StartAgent(Stream(agent)).broadcast
    case S(Ready, ASData(app, act, Some(old))) =>
      S(Ready, ASData(app, act, agent.some)) <<
        StartAgent(Stream(agent)).broadcast << StopAgent(old).broadcast
  }

  private[this] def agentForActivity(a: Activity) =
    a match {
      case sa: StateActivity => sa.agent
      case _ => None
    }

  def setActivity(a: Activity): Transit = {
    case S(Ready, ASData(app, None, ag)) =>
      import android.os.Process
      dbg("setting prio")
      Process.setThreadPriority(-5)
      val agent = agentForActivity(a) orElse initialAgent
      S(Ready, ASData(app, Some(a), ag)) << agent.map(SetAgent(_))
    case S(Ready, ASData(app, _, ag)) =>
      S(Ready, ASData(app, Some(a), ag)) << agentForActivity(a).map(SetAgent(_))
  }

  def initAgent: Transit = {
    case s @ S(Ready, ASData(app, _, Some(ag))) =>
      s << Create(Map(), None).toAgent(ag)
  }

  def activityStarted(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(app, Some(act), Some(ag))) if agent == ag =>
      s << CreateContentView.to(agent)
  }

  def setContentView(view: View): Transit = {
    case s @ S(Ready, ASData(app, Some(_), Some(ag))) =>
      s << act(_.setContentView(view))
        .map(_ => ContentViewReady(ag).broadcast).ui
  }

  def contextFun(fun: ContextFun[_]): Transit = {
    case s @ S(Ready, ASData(app, _, _)) =>
      s << fun.task(app).effect(fun.desc)
  }

  def activityFun(fun: ActivityFun[_]): Transit = {
    case s @ S(Ready, ASData(app, Some(act), _)) =>
      s << fun.task(act).effect(fun.desc)
  }

  def appCompatActivityFun(fun: AppCompatActivityFun[_]): Transit = {
    case s @ S(Ready, ASData(app, Some(act), _)) =>
      act match {
        case aca: AppCompatActivity => s << fun.task(aca).effect(fun.desc)
        case _ => s << LogError(s"executing $fun", s"wrong type $act")
      }
  }

  def activityLifecycleMessage(m: ActivityLifecycleMessage): Transit = {
    case s @ S(Ready, ASData(app, Some(act), Some(ag))) if act == m.activity =>
      m.toAgent(ag)
  }
}

case class AppStateMachine(initialAgent: Option[ActivityAgent])
extends Machine
{
  def transitions(mcomm: MComm) = AppStateTrans(initialAgent, mcomm)

  override def initialMessages = Stream(InitApp)
}

trait StateApplicationAgent
extends Agent { app =>
  lazy val appStateMachine = AppStateMachine(initialAgent)

  def transitions(mcomm: MComm) = NoTrans

  lazy val ioMachine = new IODispatcherMachine {}

  def machines = ioMachine :: appStateMachine :: Nil

  def initialAgent: Option[ActivityAgent] = None

  override def transformIn = {
    case ToAppState(m) =>
      ToMachine(m, appStateMachine)
    case m =>
      super.transformIn(m)
  }
}

object DefaultScheduler
{
  implicit lazy val scheduler: Scheduler = Scheduler.fromFixedDaemonPool(1)
}

object StatePool
extends ExecutionStrategyPool
with Logging
{
  def name = "state"

  def hook(t: Thread) = {
    t
  }

  implicit lazy val executor =
    BoundedCachedExecutor.withHook(name, 1, 10, 100, hook)
}

trait StateApplication
extends ExecutionStrategy
with Logging
{
  implicit def scheduler = DefaultScheduler.scheduler

  def pool = StatePool

  def root: StateApplicationAgent

  lazy val agents =
    CreateAgents(root)
      .infraRunLastFor("initialize agents", 10.seconds)
      .getOrElse(sys.error("agents haven't initialized"))
}
