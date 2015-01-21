package tryp.droid

import com.typesafe.config.ConfigFactory

import akka.actor.{ ActorSelection, ActorSystem, Actor, Props }

import tryp.droid.util.CallbackMixin

trait AkkaComponent {
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

trait AkkaClient extends AkkaComponent
{
  def activity: Activity

  def akkativity = {
    activity match {
      case a: Akkativity ⇒ Some(a)
      case _ ⇒ None
    }
  }

  def actorSystem = akkativity flatMap { _.actorSystem }

  def core = selectActor("core")

  def mainActor = selectActor("Main")
}

trait Akkativity
extends AkkaComponent
with CallbackMixin
{ self: Activity
  with HasActivity ⇒

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

trait AkkaFragment
extends AkkaClient
with CallbackMixin
{ self: Fragment
  with tryp.droid.FragmentBase ⇒

  abstract override def onStart {
    attach()
    super.onStart
  }

  abstract override def onStop {
    detach()
    super.onStop
  }

  lazy val actor = selectActor(actorName)

  val actorName = this.className.stripSuffix("Fragment")
}

object TrypActor {
  case class AttachUi[A <: AkkaComponent](component: A)
  case class DetachUi[A <: AkkaComponent](component: A)
}

abstract class TrypActor[A <: AkkaComponent: ClassTag]
extends Actor
{
  import TrypActor._

  var attachedUi: Option[A] = None

  lazy val core = context.system.actorSelection("/user/core")

  lazy val noUiError =
    new java.lang.RuntimeException("No Ui attached to actor")

  def withUi(f: A ⇒ Ui[Any]) = {
    attachedUi.fold(Future.failed[Any](noUiError)) { comp ⇒
      f(comp).run
    }
  }

  def ui(f: A ⇒ Any) = withUi { u ⇒ Ui(f(u)) }

  def receiveUi: PartialFunction[Any, Any] = {
    case a @ AttachUi(f: A) ⇒ {
      attachedUi = Some(f)
      inject()
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
      case Messages.Inject(attr, value) ⇒
        updateParameter(attr, value)
      case AttachUi(_) ⇒
      case DetachUi(_) ⇒
      case a ⇒ Log.w(s"Unhandled message in ${this.className}: ${a}")
    }
  }

  val parameters: MMap[String, Any] = MMap()
  val parameterDispatch: MMap[String, (Any ⇒ Unit)] = MMap()

  protected def addParameter[B: ClassTag](name: String,
    dispatch: A ⇒ (B ⇒ Ui[Any]))
    {
    val setter: Any ⇒ Unit = { v ⇒
      v match {
        case b: B ⇒
          withUi { a ⇒ meta.Debug.rescued(dispatch(a)(b)) }
        case _ ⇒
          Log.e(s"Wrong parameter type in ${this.className} for ${name}")
      }
    }
    parameterDispatch(name) = setter
  }

  protected def updateParameter(name: String, value: Any) {
    if (parameterDispatch contains name) {
      parameters(name) = value
    }
    else {
      val valStr = (value == null) ? "Null" / value.toString
      Log.e(s"Tried to update unregistered parameter '${name}' with value " +
        s"'${valStr}' in '${this.className}'")
    }
  }

  protected def inject() {
    parameterDispatch foreach {
      case (name, setter) ⇒ parameters lift(name) foreach(setter)
    }
  }
}

trait AkkaAdapter
extends AkkaClient
{
  def actor = None
}

abstract class TrypActivityActor[A <: TrypActivity: ClassTag]
extends TrypActor[A]
{
  import Messages._
  import TrypActor._

  def receiveBasic(m: Any) = {
    m match {
      case Back() ⇒
        ui { _.back() }
      case Navigation(target) ⇒
        ui { _.navigate(target) }
      case a ⇒ unhandled(a)
    }
  }
}

abstract class TrypDrawerActivityActor[A <: TrypDrawerActivity: ClassTag]
extends TrypActivityActor[A]
{
  import Messages._
  import TrypActor._

  override def receiveBasic(m: Any) = {
    m match {
      case ToolbarTitle(title) ⇒
        ui { _.toolbarTitle(title) }
      case ToolbarView(view) ⇒
        ui { _.toolbarView(view) }
      case a ⇒ super.receiveBasic(a)
    }
  }
}
