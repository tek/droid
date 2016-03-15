package tryp.droid

import com.google.android.gms.maps.GoogleMap

import akka.actor.{Props, ActorLogging}

abstract trait ShowMap
extends AkkaComponent
{
  def setMap(map: GoogleMap)
}

class ShowMapActor
extends TrypActor[ShowMap]
with ActorLogging
{
  import TrypActor._

  def receive = receiveUi andThen {
    case Messages.MapReady(map) =>
      ui(_.setMap(map))
    case a =>
      unhandled(a)
  }
}

object ShowMapActor {
  def props = Props(new ShowMapActor)
}
