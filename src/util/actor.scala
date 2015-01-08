package tryp.droid

import scala.reflect.ClassTag

import com.typesafe.config.ConfigFactory

import akka.actor.{ ActorSelection, ActorSystem, Actor, Props }

import tryp.droid.util.CallbackMixin

trait AkkaComponent extends CallbackMixin {
  def actor: Option[ActorSelection]

  def attach() {
    actor ! TrypActor.AttachUi(this)
  }

  def detach() {
    actor ! TrypActor.DetachUi(this)
  }

  def selectActor(name: String) = {
    actorSystem map { _.actorSelection(s"/user/${name}") }
  }

  def actorSystem: Option[ActorSystem]
}

trait Akkativity extends AkkaComponent
{ self: Activity
  with view.HasActivity ⇒

  lazy val actorSystemName = res.string("app_handle").trim

  lazy val actorSystemInst = ActorSystem(
    actorSystemName,
    ConfigFactory.load(getApplication.getClassLoader),
    getApplication.getClassLoader)

  def actorSystem = Option(actorSystemInst)

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    actors
  }

  abstract override def onStart {
    coreActor
    attach()
    super.onStart
  }

  abstract override def onStop {
    detach()
    super.onStop
  }

  lazy val actors = Map(createActors: _*)

  def createActors =
    actorsProps map(createActor)

  def createActor(props: Props) = {
    val name = props.actorClass.className.stripSuffix("Actor")
    val a = actorSystemInst.actorOf(props, name)
    (name → a)
  }

  def actor = actorSystem map { _.actorSelection("/user/core") }

  lazy val coreActor = actorSystemInst.actorOf(coreActorProps, "core")

  val coreActorProps: Props

  def actorsProps: Seq[Props]
}

trait AkkaFragment extends AkkaComponent
{ self: Fragment
  with tryp.droid.FragmentBase ⇒

  def akkativity = {
    activity match {
      case a: Akkativity ⇒ Some(a)
      case _ ⇒ None
    }
  }

  abstract override def onStart {
    attach()
    super.onStart
  }

  abstract override def onStop {
    detach()
    super.onStop
  }

  def actorSystem = akkativity flatMap { _.actorSystem }

  def core = selectActor("core")

  lazy val actor = selectActor(actorName)

  val actorName = this.className.stripSuffix("Fragment")
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

  def ui(f: A ⇒ Any) = withUi { u ⇒ Ui(f(u)) }

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
    a match {
      case AttachUi(_) ⇒
      case DetachUi(_) ⇒
      case a ⇒ Log.w(s"Unhandled message in ${this.className}: ${a}")
    }
  }
}

abstract class TrypDrawerActivityActor[A <: TrypDrawerActivity: ClassTag]
extends TrypActor[A]
{
  import Messages._
  import TrypActor._

  def receiveBasic(m: Any) = {
    m match {
      case ToolbarTitle(title) ⇒
        ui { _.toolbarTitle(title) }
      case ToolbarView(view) ⇒
        ui { _.toolbarView(view) }
      case Navigation(target) ⇒
        ui { _.navigate(target) }
      case a ⇒ unhandled(a)
    }
  }
}
