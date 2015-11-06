package tryp.droid

import com.typesafe.config.ConfigFactory

import akka.actor.{ ActorSelection, ActorSystem, Actor, Props }

object Akka
{
  def newSystem(name: String, loader: ClassLoader) = 
    ActorSystem(name, ConfigFactory.load(loader), loader)
}

trait AkkaComponent
{

  def actor(props: Props): ActorSelection = {
    selectActor(props.actorName)
  }

  def selectActor(name: String) = {
    actorSystem.actorSelection(s"/user/${name}")
  }

  def actorSystem: ActorSystem

  implicit def ec = actorSystem.dispatcher

  def core = selectActor("core")
}

trait AkkaClient extends AkkaComponent
{
  def activity: Activity

  def akkativity = {
    activity match {
      case a: Akkativity ⇒ Some(a)
      case _ ⇒
        Log.e(s"No Akkativity access in $this")
        None
    }
  }

  def actorSystem = {
    akkativity
      .some(_.actorSystem)
      .none(Akka.newSystem("dummy", activity.getClassLoader))
  }

  def mainActor = actor(MainActor.props)

  def actors: Seq[Props] = Seq()

  def attach() {
    val msg = TrypActor.AttachUi(this)
    actors foreach { props ⇒
      selectActor(props.actorName) ! msg
    }
  }

  def detach() {
    val msg = TrypActor.DetachUi(this)
    actors foreach { p ⇒
      selectActor(p.actorName) ! msg
    }
  }
}

trait Akkativity
extends AkkaComponent
with CallbackMixin
{ self: Activity
  with HasActivity ⇒

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    mainActors
  }

  abstract override def onStart() {
    coreActor ! TrypActor.AttachUi(this)
    super.onStart()
  }

  abstract override def onStop() {
    coreActor ! TrypActor.DetachUi(this)
    super.onStop
  }

  lazy val actorSystem = Akka.newSystem("tryp", getClassLoader)

  lazy val mainActors = Map(createActors: _*)

  def createActors = actorsProps map(createActor)

  def createActor(props: Props) = {
    val name = props.actorName
    name → actorSystem.actorOf(props, name)
  }

  lazy val coreActor = actorSystem.actorOf(coreActorProps, "core")

  val coreActorProps: Props

  def actorsProps: Seq[Props]
}

trait AkkaFragment
extends AkkaClient
with CallbackMixin
{ self: Fragment
  with tryp.droid.FragmentBase ⇒

  abstract override def onStart() {
    attach()
    super.onStart()
  }

  abstract override def onStop() {
    detach()
    super.onStop()
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
with AkkaComponent
{
  import TrypActor._

  def actorSystem = context.system

  var attachedUi: Option[A] = None

  def mainActor = actor(MainActor.props)

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
          withUi { a ⇒ dispatch(a)(b) }
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
      val valStr = (value == null) ? "Null" | value.toString
      Log.e(s"Tried to update unregistered parameter '${name}' with value " +
        s"'${valStr}' in '${this.className}'")
    }
  }

  protected def inject() {
    parameterDispatch foreach {
      case (name, setter) ⇒ parameters remove(name) foreach(setter)
    }
  }
}

trait AkkaAdapter
extends AkkaClient
{
}

abstract class TrypActivityActor[A <: StatefulActivity: ClassTag]
extends TrypActor[A]
{
  import Messages._
  import TrypActor._

  def receiveBasic(m: Any) = {
    m match {
      case Back(result) ⇒
        ui { _.back() } onComplete { _ ⇒
          result foreach { mainActor ! Messages.Result(_) }
        }
      case Navigation(target) ⇒
        ui { _.navigate(target) }
      case Transitions(sets) ⇒
        ui { _.addTransitions(sets) }
      case ShowDetails(data) ⇒
        ui(_.showDetails(data))
      case Toast(id) ⇒
        ui(_.toast(id))
      case a ⇒ unhandled(a)
    }
  }
}

abstract class TrypDrawerActivityActor
[A <: StatefulActivity with TrypDrawerActivity: ClassTag]
extends TrypActivityActor[A]
{
  import Messages._
  import TrypActor._

  override def receiveBasic(m: Any) = {
    m match {
      case ToolbarTitle(title) ⇒
        withUi(_.toolbarTitle(title))
      case ToolbarView(view) ⇒
        ui { _.toolbarView(view) }
      case DrawerClick(action) ⇒
        ui(_.drawerClick(action))
      case a ⇒ super.receiveBasic(a)
    }
  }
}

class AkkaAndroidLogger extends Actor {
  import akka.event.Logging._

  def receive = {
    case Error(cause, logSource, logClass, message) ⇒
      Log.e(s"$message [$logSource]: $cause")
    case Warning(logSource, logClass, message) ⇒
      Log.w(s"$message [$logSource]")
    case Info(logSource, logClass, message) ⇒
      Log.i(s"$message [$logSource]")
    case Debug(logSource, logClass, message) ⇒
      Log.d(s"$message [$logSource]")
    case InitializeLogger(_) ⇒
      Log.d("Logging started")
      sender ! LoggerInitialized
  }
}
