package tryp.droid.meta

import akka.actor._

trait AkkaExt
extends tryp.meta.MetadataExt
{
  implicit class `Option of ActorSelection`[A <: ActorSelection](a: Option[A])
  {
    def !(msg: AnyRef) {
      a foreach { _ ! msg }
    }
  }

  implicit class `Props extensions`(p: Props)
  {
    def actorName = p.actorClass.className.stripSuffix("Actor")
  }
}
