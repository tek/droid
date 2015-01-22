package tryp.droid

import akka.actor.Props

class LogActor
extends TrypActor[LogFragment]
{
  import TrypActor._

  def receive = receiveUi andThen {
    case Messages.Log(_) ⇒
      ui { _.update() }
    case a ⇒
      unhandled(a)
  }
}

object LogActor {
  def props = Props(new LogActor)
}
