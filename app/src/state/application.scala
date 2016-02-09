package tryp
package droid
package state

import cats.syntax.all._

trait AppStateMachine
extends Machine

object AppStateMachine
{
  case class SetActivity(activity: Activity)
  extends Message

  case class SetAgent(agent: Activity ⇒ AppStateActivityAgent)
  extends Message

  case object Ready
  extends BasicState

  case class ASData(activity: Option[Activity], agent: Option[AppStateActivityAgent])
  extends Data
}
import AppStateMachine._

trait StateApplication
extends Application
with RootAgent
{
  self: android.app.Application ⇒

    lazy val machine = new AppStateMachine {
      def handle = "app_state"

      def admit: Admission = {
        case Create(_, _) ⇒ create
        case SetActivity(a) ⇒ setActivity(a)
        case SetAgent(a) ⇒ setAppStateActivityAgent(a)
      }

      def create: Transit = {
        case s ⇒ s
      }

      def setAppStateActivityAgent(a: Activity ⇒ AppStateActivityAgent): Transit = {
        case S(Ready, ASData(Some(act), _)) ⇒
          val agent = a(act)
          S(Ready, ASData(act.some, agent.some)) <<
            stateEffect("start activity agent")(agent.start()) <<
            stateEffect("set activity view")(agent.setView())
      }

      def setActivity(a: Activity): Transit = {
        case S(Pristine, NoData) ⇒
          S(Ready, ASData(a.some, None))
        case S(Ready, ASData(_, ag)) ⇒
          S(Ready, ASData(a.some, ag))
      }
    }

    override def machines = machine %:: super.machines

    abstract override def onCreate() {
      super.onCreate()
      forkAgent()
      send(Create(Map(), None))
    }

    def setActivity(act: Activity) = {
      send(SetActivity(act))
      send(SetAgent(defaultAgent))
    }

    def defaultAgent(a: Activity): AppStateActivityAgent
}
