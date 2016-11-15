package tryp
package droid
package state

import scalaz.stream._
import Process._

import android.support.v7.app.AppCompatActivity

import iota.ViewTree

import tryp.state.AgentStateData._
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

  case class OnStart(activity: Activity)
  extends Message

  case class OnResume(activity: Activity)
  extends Message
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
    case SetContentView(view, sender) => setContentView(view)
    case SetContentTree(view, sender) => setContentView(view.container)
    case f @ ContextFun(_, _) => contextFun(f)
    case f @ ActivityFun(_, _) => activityFun(f)
    case f @ AppCompatActivityFun(_, _) => appCompatActivityFun(f)
    // case t @ DbTask(_) => dbTask(t)
    case t @ ECTask(_) => ecTask(t)
  }

  def startActivity(agent: ActivityAgent): Transit = {
    case s @ S(Ready, ASData(_, _)) =>
      s << con(_.startActivityCls(agent.activityClass)) <<
        SetAgent(agent)
  }

  def setAgent(agent: ActivityAgent): Transit = {
    case S(Ready, ASData(act, None)) =>
      S(Ready, ASData(act, agent.some)) << AddSub(Nel(agent)).toAgentMachine
    case S(Ready, ASData(act, Some(old))) =>
      S(Ready, ASData(act, agent.some)) << AddSub(Nel(agent)).toAgentMachine <<
        StopSub(Nel(old))
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
      s << ag.publish(Create(Map(), None))
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

  def ecTask(task: ECTask[_]): Transit = _ << task.effect(ec)
}

trait StateApplicationAgent
extends RootAgent { app =>
  override def handle = "state_app"

  lazy val appStateMachine = new AppStateMachine {
    def initialAgent = app.initialAgent
    def dbInfo = app.dbInfo
  }

  lazy val ioMachine = new IODispatcher {}

  override def machines = ioMachine :: appStateMachine :: super.machines

  def initialAgent: Option[ActivityAgent] = None

  def dbInfo: Option[DbInfo] = None

  override def extraAdmit = super.extraAdmit orElse {
    case a @ SetContentView(_, _) => _ << appStateMachine.sendP(a)
    case a @ SetContentTree(_, _) => _ << appStateMachine.sendP(a)
  }

  def setActivity(act: Activity) = {
    log.debug(s"setting activity $act")
    scheduleOne(SetActivity(act).toLocal)
  }

  def onStart(activity: Activity) =
    appStateMachine.send(OnStart(activity))

  def onResume(activity: Activity) =
    appStateMachine.send(OnResume(activity))
}

trait StateApplication
{
  def stateAppAgent: StateApplicationAgent
}
