package tryp.droid

import scala.reflect.ClassTag

import com.typesafe.config.ConfigFactory

import akka.actor.{ ActorSelection, ActorSystem, Actor, Props }

import macroid.Ui

import tryp.droid.util.CallbackMixin
import tryp.droid.activity.TrypActivity
import tryp.droid.AkkaExt._

trait AkkaComponent extends CallbackMixin {
  def actor: Option[ActorSelection]

  abstract override def onStart = {
    super.onStart
    actor ! TrypActor.AttachUi(this)
  }

  abstract override def onStop = {
    super.onStop
    actor ! TrypActor.DetachUi(this)
  }
}

trait Akkativity extends AkkaComponent { self: android.app.Activity ⇒
  val actorSystemName: String

  lazy val actorSystem = ActorSystem(
    actorSystemName,
    ConfigFactory.load(getApplication.getClassLoader),
    getApplication.getClassLoader)

  abstract override def onStart {
    coreActor
    super.onStart
  }

  def actor = Option(actorSystem.actorSelection("/user/core"))

  lazy val coreActor = actorSystem.actorOf(coreActorProps, "core")

  val coreActorProps: Props
}

trait AkkaFragment extends AkkaComponent { self: tryp.droid.FragmentBase ⇒
  def akkativity = {
    activity match {
      case a: Akkativity ⇒ Some(a)
      case _ ⇒ None
    }
  }

  def actorSystem = akkativity map { _.actorSystem }

  def core = actorSystem map { _.actorSelection("/user/core") }
}

object TrypActor {
  case class AttachUi[A <: AkkaComponent](component: A)
  case class DetachUi[A <: AkkaComponent](component: A)
}

abstract class TrypActor[A <: AkkaComponent: ClassTag]
extends Actor {
  import TrypActor._

  var attachedUi: Option[A] = None

  lazy val core = context.system.actorSelection("/user/core")

  def withUi(f: A ⇒ Ui[Any]) = attachedUi.fold(()) { comp ⇒
    f(comp).run
  }

  def receiveUi: PartialFunction[Any, Any] = {
    case a @ AttachUi(f: A) ⇒ {
      attachedUi = Some(f)
      a
    }
    case d @ DetachUi(f: A) if Some(f) == attachedUi ⇒ {
      attachedUi = None
      d
    }
    case x ⇒ x
  }

  override def unhandled(a: Any) {
    Log.w(s"Unhandled message in ${this.className}: ${a}")
  }
}
