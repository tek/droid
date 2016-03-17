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

  case class SetAgent(agent: Activity => AppStateActivityAgent)
  extends Message

  case object SetMainView
  extends Message

  case object Ready
  extends BasicState

  case object AgentInitialized
  extends Message

  case class ASData(activity: Option[Activity],
    agent: Option[AppStateActivityAgent])
  extends Data
}
import AppState._

@Publish(AddSub)
trait AppStateMachine
extends Machine
{
  def handle = "app_state"

  lazy val mainView = async.unboundedQueue[view.FreeIO[_ <: View]]

  def admit: Admission = {
    case SetActivity(a) => setActivity(a)
    case SetAgent(a) => setAppStateActivityAgent(a)
    case SubAdded => initAgent
    case AgentInitialized => setMainView
  }

  def setAppStateActivityAgent
  (a: Activity => AppStateActivityAgent): Transit = {
    case S(Ready, ASData(Some(act), _)) =>
      val agent = a(act)
      S(Ready, ASData(act.some, agent.some)) <<
        AddSub(Nes(agent))
  }

  def setActivity(a: Activity): Transit = {
    case S(Pristine, NoData) =>
      S(Ready, ASData(a.some, None))
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
}

trait StateApplication
extends Application
with RootAgent
{
  self: android.app.Application =>

    def handle = "state_app"

    lazy val machine = new AppStateMachine { }

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
      publishAll(Nes(SetActivity(act), SetAgent(defaultAgent)))
      machine.mainView.dequeue.take(1)
      // publish(SetActivity(act), SetAgent(defaultAgent)) flatMap { _ =>
      //   machine.mainView.dequeue.take(1)
      // }
    }

    def view = {
      machine.mainView.dequeue.take(1)
    }

    def defaultAgent(a: Activity): AppStateActivityAgent
}
