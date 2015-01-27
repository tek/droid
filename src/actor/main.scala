package tryp.droid

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Props

class MainActor
extends TrypActor[MainFragment]
{
  import TrypActor._

  def receive = receiveUi andThen {
    case Messages.Back() ⇒
      withUi { _.back() } onComplete {
        case Failure(_) ⇒
          core ! Messages.Back()
        case Success(_) ⇒
      }
    case Messages.DataLoaded() ⇒
      ui { _.dataLoaded() }
    case a ⇒
      unhandled(a)
  }
}

object MainActor {
  def props = Props(new MainActor)
}
