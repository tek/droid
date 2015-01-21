package tryp.droid

import akka.actor.Props

class DrawerActor
extends TrypActor[DrawerFragment]
{
  import TrypActor._

  addParameter("navigation", { _.setNavigation _ })

  def receive = receiveUi andThen {
    case Messages.Navigation(_) ⇒
      withUi { _.navigated() }
    case a ⇒
      unhandled(a)
  }
}

object DrawerActor {
  def props = Props(new DrawerActor)
}
