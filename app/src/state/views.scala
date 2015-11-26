package tryp
package droid
package state

import concurrent.duration._

trait UiDispatcher
extends Machine[HNil]
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
extends ViewState[View]
{
  def layoutIOT = w[View]
}

trait HasContextAgent
extends Agent
with HasContext
{
  implicit def uiCtx: AndroidUiContext = AndroidContextUiContext.default

  lazy val uiMachine = new UiDispatcher {}

  implicit def ec: EC

  val viewState: ViewState[_ <: View]

  override def machines = uiMachine :: viewState :: super.machines
}

trait HasActivityAgent
extends HasContextAgent
with HasActivity
{
  override implicit def uiCtx: AndroidUiContext =
    AndroidActivityUiContext.default

  override def machines = activityMachine :: super.machines

  protected lazy val activityMachine = new DroidState
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

trait FragmentAgent
extends HasActivityAgent
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

trait ActivityAgent
extends TrypActivity
with HasActivityAgent
{
  lazy val viewState: ViewState[View] = new DummyViewState {}

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
