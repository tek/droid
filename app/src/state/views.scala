package tryp
package droid
package state

import shapeless._

import concurrent.duration._

trait UiDispatcher
extends SimpleMachine
{
  def handle = "ui"

  val admit: Admission = {
    case UiTask(ui, timeout) ⇒ {
      case s ⇒
        s << Task(scala.concurrent.Await.result(ui.run, timeout))
    }
  }
}

trait DummyViewMachine
extends SimpleViewMachine
{
  def layoutIO = w[View]
}

trait HasContextAgent
extends Agent
with HasContext
{
  implicit def uiCtx: AndroidUiContext = AndroidContextUiContext.default

  lazy val uiMachine = new UiDispatcher {}

  override def machines = uiMachine :: super.machines
}

trait HasActivityAgent
extends HasContextAgent
with ActivityAccess
{
  override implicit def uiCtx: AndroidUiContext =
    AndroidActivityUiContext.default

  override def machines = activityMachine :: super.machines

  protected lazy val activityMachine = new SimpleDroidMachine
  {
    override def description = "activity access state"

    def handle = "activity"

    def toast(id: String): Transit = {
      case s ⇒
        s << uiCtx.notify(id)
    }

    val admit: Admission = {
      case Toast(id) ⇒ toast(id)
    }
  }

  lazy val mediator = activitySub[ActivityAgent] getOrElse fallbackMediator
}

trait FragmentAgent
extends HasActivityAgent
with CallbackMixin
{
  self: FragmentBase ⇒

  override implicit def uiCtx = AndroidFragmentUiContext.default(self)

  abstract override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    initMachines()
    send(Create(self.arguments, Option(saved)))
  }

  abstract override def onResume() {
    super.onResume()
    send(Resume)
  }
}

trait ActivityAgent
extends ActivityBase
with HasActivityAgent
with Mediator
{
  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    initMachines()
    send(Create(Map(), Option(saved)))
  }

  override def onResume() {
    super.onResume()
    send(Resume)
  }

  override lazy val mediator = this
}
