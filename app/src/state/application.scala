package tryp
package droid
package state

import cats.syntax.all._

import scalaz.stream._
import Process._
import AgentStateData._

object AppState
{
  case class SetActivity(activity: Activity)
  extends Message

  case class SetActivityAgent(agent: Activity => ASAAgentBase)
  extends Message

  case class StartActivity(agent: ASAAgentBase)
  extends Message

  case class SetAgent(agent: ASAAgentBase)
  extends Message

  case object SetMainView
  extends Message

  case class ContextTask[A: Operation](f: AndroidActivityUiContext => A)
  extends Message
  {
    def task(implicit a: Activity) = {
      Task(f(AndroidActivityUiContext.default)) map(_.toResult)
    }
  }

  case object Ready
  extends BasicState

  case object AgentInitialized
  extends Message

  case class ASData(activity: Option[Activity],
    agent: Option[ASAAgentBase])
  extends Data
}
import AppState._

@Publish(AddSub)
trait AppStateMachine
extends Machine
{
  def handle = "app_state"

  lazy val mainView = async.unboundedQueue[view.FreeIO[_ <: View]]

  def initialAgent(a: Activity): ASAAgentBase

  def admit: Admission = {
    case SetActivity(a) => setActivity(a)
    case SetActivityAgent(a) => setActivityAgent(a)
    case SetAgent(a) => setAgent(a)
    case StartActivity(a) => startActivity(a)
    case SubAdded => initAgent
    case AgentInitialized => setMainView
    case t @ ContextTask(_) => contextTask(t)
  }

  def setActivityAgent(a: Activity => ASAAgentBase): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      val agent = a(act)
      s << SetAgent(a(act))
  }

  def startActivity(a: ASAAgentBase): Transit = {
    case s @ S(Ready, ASData(_, _)) => 
      s << ContextTask(_.startActivity(a.activityClass)) << SetAgent(a)
  }

  def setAgent(agent: ASAAgentBase): Transit = {
    case S(Ready, ASData(act, None)) =>
      S(Ready, ASData(act, agent.some)) << AddSub(Nes(agent))
    case S(Ready, ASData(act, Some(old))) =>
      S(Ready, ASData(act, agent.some)) << AddSub(Nes(agent)) <<
        StopSub(Nes(old))
  }

  def setActivity(a: Activity): Transit = {
    case S(Pristine, NoData) =>
      S(Ready, ASData(a.some, None)) << SetActivityAgent(initialAgent)
    case S(Ready, ASData(_, ag)) =>
      S(Ready, ASData(a.some, ag))
  }

  def initAgent: Transit = {
    case s @ S(Ready, ASData(_, Some(ag))) =>
      s << ag.startP << AgentInitialized
  }

  def setMainView: Transit = {
    case s @ S(Ready, ASData(_, Some(ag))) =>
      s << ag.safeViewIO.to(mainView.enqueue).forkEffect("set main view")
  }

  def contextTask(task: ContextTask[_]): Transit = {
    case s @ S(Ready, ASData(Some(act), _)) =>
      s << task.task(act)
  }
}

trait StateApplication
extends Application
with RootAgent { app: android.app.Application =>

    def handle = "state_app"

    lazy val machine = new AppStateMachine {
      def initialAgent(a: Activity) = app.initialAgent(a)
    }

    override def machines = machine %:: super.machines

    abstract override def onCreate() {
      forkAgent()
      super.onCreate()
    }

    // FIXME deadlock when publishing messages in the returned Process
    // FIXME also deadlocks when called before machine is running
    // TODO before publishing, wait for machine to fire up
    // or even better: find out why this deadlocks
    def setActivity(act: Activity) = {
      publishAll(Nes(SetActivity(act)))
      machine.mainView.dequeue.take(1)
      // publish(SetActivity(act), SetActivityAgent(initialAgent)) flatMap { _ =>
      //   machine.mainView.dequeue.take(1)
      // }
    }

    def view = {
      machine.mainView.dequeue.take(1)
    }

    def initialAgent(a: Activity): ASAAgentBase
}
