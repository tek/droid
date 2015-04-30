package tryp.droid

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Props

class MainActor
extends TrypActor[MainFragment]
{
  import TrypActor._

  def receive = receiveUi andThen {
    case Messages.Back(result) ⇒
      withUi { _.back() } onComplete {
        case Failure(_) ⇒
          core ! Messages.Back()
        case Success(_) ⇒
          result foreach { self ! _ }
      }
    case Messages.DataLoaded() ⇒
      ui { _.dataLoaded() }
    case Messages.Scrolled(view, dy) ⇒
      ui { _.scrolled(view, dy) }
    case Messages.Result(data: Any) ⇒
      ui { _.result(data) }
    case a ⇒
      unhandled(a)
  }
}

object MainActor {
  def props = Props(new MainActor)
}
