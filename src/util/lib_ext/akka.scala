package tryp.droid

import akka.actor.ActorSelection

object AkkaExt {
  implicit class `Option of ActorSelection`[A <: ActorSelection](a: Option[A])
  {
    def !(msg: AnyRef) {
      a foreach { _ ! msg }
    }
  }
}
