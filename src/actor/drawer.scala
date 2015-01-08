package tryp.droid

import akka.actor.Props

class DrawerActor
extends TrypActor[DrawerFragment]
{
  import TrypActor._

  def receive = receiveUi andThen {
    case m: Messages.Navigation ⇒
      ui { _.navigated() }
    case a ⇒
      unhandled(a)
  }
}

object DrawerActor {
  def props = Props(new DrawerActor)
}
