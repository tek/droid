package tryp
package droid

import concurrent.duration._

import scalaz._, Scalaz._
import concurrent.Task

import ViewState._

trait Stateful
extends DbAccess
with ViewStateImplicits
{
  implicit def uiCtx: AndroidUiContext

  implicit val broadcast: Broadcaster = new Broadcaster(sendAll)

  lazy val logImpl = new StateImpl
  {
    override def description = "log state"

    def logError(msg: String): ViewTransition = {
      case s ⇒
        Log.e(msg)
        s
    }

    def logInfo(msg: String): ViewTransition = {
      case s ⇒
        Log.i(msg)
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

  lazy val uiImpl = new StateImpl
  {
    override def description = "ui state"

    val transitions: ViewTransitions = {
      case UiTask(ui, timeout) ⇒ {
        case s ⇒
          s << Task(scala.concurrent.Await.result(ui.run, timeout))
      }
    }
  }

  def impls: List[StateImpl] = logImpl :: uiImpl :: Nil

  def allImpls[A](f: StateImpl ⇒ A) = impls map(f)

  def send(msg: Message) = allImpls(_.send(msg))

  def sendAll(msgs: NonEmptyList[Message]) = msgs foreach(send)

  val ! = send _

  def runState() = allImpls(_.runFsm())

  def killState() {
    allImpls(_.kill())
  }

  def joinState() {
    allImpls(_.join())
  }
}

trait StatefulHasActivity
extends Stateful
with HasActivity
{
  implicit def uiCtx: AndroidUiContext = AndroidActivityUiContext.default

  override def impls = activityImpl :: super.impls

  protected lazy val activityImpl = new StateImpl
  {
    override def description = "activity access state"

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
  self: TrypFragment ⇒

  override implicit def uiCtx = AndroidFragmentUiContext.default(self)

  abstract override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    runState()
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
  // TODO impl log level
  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    runState()
    // logImpl ! LogLevel(LogLevel.DEBUG)
    send(Create(Map(), Option(saved)))
  }

  override def onResume() {
    super.onResume()
    send(Resume)
  }
}
