package tryp.droid.util

import akka.actor.ActorSelection

trait AkkaExt {
  implicit class `Option of ActorSelection`[A <: ActorSelection](a: Option[A])
  {
    def !(msg: AnyRef) {
      a foreach { _ ! msg }
    }
  }
}
