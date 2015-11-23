package tryp
package droid

import concurrent.duration._

import scalaz._, Scalaz._, stream._, Process._, async._, mutable._
import concurrent._

import State._

trait LogImpl
extends StateImpl
{
  def handle = "log"

  def logError(msg: String): ViewTransition = {
    case s ⇒
      log.error(msg)
      s
  }

  def logInfo(msg: String): ViewTransition = {
    case s ⇒
      log.info(msg)
      s
  }

  val transitions: ViewTransitions = {
    case m: LogError ⇒ logError(m.message)
    case m: LogFatal ⇒ logError(m.message)
    case m: LogInfo ⇒ logInfo(m.message)
    case UnknownResult(msg) ⇒ logInfo(msg.toString)
    case m: EffectSuccessful ⇒ logInfo(m.message)
    case m: Loggable ⇒ logInfo(m.toString)
  }
}

case class MessageTopic(topic: Topic[Message] = async.topic[Message]())
extends AnyVal
{
  def subscribe = topic.subscribe
  def publish = topic.publish
}

trait Stateful
extends StateImplicits
{
  implicit lazy val strat: Strategy = Strategy.Naive

  implicit lazy val messageTopic = MessageTopic()

  lazy val logImpl = new LogImpl {}

  def impls: List[StateImpl] = logImpl :: Nil

  def allImpls[A](f: StateImpl ⇒ A) = impls map(f)

  def send(msg: Message) = sendAll(msg.wrapNel)

  def sendAll(msgs: NonEmptyList[Message]) = {
    messageIn.enqueueAll(msgs.toList) !? s"enqueue messages $msgs in $this"
  }

  val ! = send _

  def runState() = {
    runPublisher()
    allImpls(_.runFsm())
  }

  protected def initState() = {
    runState()
    postRunState()
  }

  protected def postRunState(): Unit = ()

  lazy val messageIn = unboundedQueue[Message]

  lazy val messageOut = impls foldMap(_.messageOut.dequeue)

  private[this] def runPublisher() = {
    messageOut
      .merge(messageIn.dequeue)
      .to(messageTopic.publish)
      .run
      .infraRunAsync("message publisher")
  }

  def killState() = {
    allImpls(_.kill())
  }

  def joinState() = {
    allImpls(_.join())
  }
}

trait UiDispatcher
extends StateImpl
{
  def handle = "ui"

  val transitions: ViewTransitions = {
    case UiTask(ui, timeout) ⇒ {
      case s ⇒
        s << Task(scala.concurrent.Await.result(ui.run, timeout))
    }
  }
}

trait DummyViewState
extends ViewState
{
  def layoutIOT = w[View]
}

trait StatefulView
extends Stateful
with HasContext
{
  implicit def uiCtx: AndroidUiContext = AndroidContextUiContext.default

  lazy val uiImpl = new UiDispatcher {}

  implicit def ec: EC

  def viewState: ViewState

  override def impls = uiImpl :: viewState :: super.impls
}

trait StatefulHasActivity
extends StatefulView
with HasActivity
{
  override implicit def uiCtx: AndroidUiContext =
    AndroidActivityUiContext.default

  override def impls = activityImpl :: super.impls

  protected lazy val activityImpl = new DroidState
  {
    override def description = "activity access state"

    def handle = "activity"

    def toast(id: String): ViewTransition = {
      case s ⇒
        s << uiCtx.notify(id)
    }

    val transitions: ViewTransitions = {
      case Toast(id) ⇒ toast(id)
    }
  }
}

trait StatefulFragment
extends StatefulHasActivity
with CallbackMixin
{
  self: FragmentBase ⇒

  override implicit def uiCtx = AndroidFragmentUiContext.default(self)

  abstract override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    initState()
    send(Create(self.arguments, Option(saved)))
  }

  abstract override def onResume() {
    super.onResume()
    send(Resume)
  }
}

trait StatefulActivity
extends TrypActivity
with StatefulHasActivity
{
  def viewState: ViewState = new DummyViewState {}

  // TODO impl log level
  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    initState()
    // logImpl ! LogLevel(LogLevel.DEBUG)
    send(Create(Map(), Option(saved)))
  }

  override def onResume() {
    super.onResume()
    send(Resume)
  }
}
