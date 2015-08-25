package tryp.droid

import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.Props

class MainActor
extends TrypActor[MainFragment]
{
  import TrypActor._

  var lastResult: Option[Any] = None

  def receive = receiveUi andThen {
    case Messages.Back(result) ⇒
      withUi { _.back() } onComplete {
        case Failure(_) ⇒
          core ! Messages.Back(result)
        case Success(_) ⇒
          result foreach { self ! Messages.Result(_) }
      }
    case Messages.DataLoaded() ⇒
      ui { _.dataLoaded() }
    case Messages.Scrolled(view, dy) ⇒
      ui { _.scrolled(view, dy) }
    case Messages.Result(data: Any) ⇒
      lastResult = Some(data)
    case Messages.StartAsyncTask(task: Future[_]) ⇒
      ui { _.startAsyncTask() }
    case Messages.CompleteAsyncTask(task: Future[_]) ⇒
      ui { _.completeAsyncTask() }
    case AttachUi(_) ⇒
      lastResult foreach { r ⇒
        ui { _.result(r) }
        lastResult = None
      }
    case a ⇒
      unhandled(a)
  }
}

object MainActor {
  def props = Props(new MainActor)
}
