package tryp
package droid
package state

import scalaz._, Scalaz._, concurrent._, stream._
import android.widget._

import java.util.concurrent._

import shapeless._

trait UiDispatcher
extends Machine
{
  def handle = "ui"

  val admit: Admission = {
    case UiTask(ui, timeout) => {
      case s =>
        s << Task(scala.concurrent.Await.result(ui.run, timeout))
    }
    case ViewStreamTask(io, timeout) => {
      case s =>
        s << io.unsafePerformIOMain(timeout).effect("perform ViewStream IO")
    }
  }
}

trait DummyViewMachine
extends ViewMachine
{
  def layoutIO = w[View]
}

trait HasContextAgent
extends RootAgent
with HasContext
{
  implicit def uiCtx: AndroidUiContext = AndroidContextUiContext.default

  lazy val uiMachine = new UiDispatcher {}

  override def machines = uiMachine %:: super.machines
}

trait HasActivityAgent
extends HasContextAgent
with ActivityAccess
{
  override implicit def uiCtx: AndroidUiContext =
    AndroidActivityUiContext.default

  override def machines = activityMachine %:: super.machines

  protected lazy val activityMachine = new SimpleDroidMachine
  {
    override def description = "activity access state"

    def handle = "activity"

    def toast(id: String): Transit = {
      case s =>
        s << uiCtx.notify(id)
    }

    val admit: Admission = {
      case Toast(id) => toast(id)
    }
  }
}

trait FragmentAgent
extends HasActivityAgent
with CallbackMixin
{
  self: FragmentBase =>

  def handle = "fragment"

  override implicit def uiCtx = AndroidFragmentUiContext.default(self)

  abstract override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    forkAgent()
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
with RootAgent
{
  def handle = "activity"

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    forkAgent()
    send(Create(Map(), Option(saved)))
  }

  override def onResume() {
    super.onResume()
    send(Resume)
  }
}

trait ViewAgent
extends HasActivityAgent
with view.ExtViews
{
  def viewMachine: ViewMachine

  def title: String

  override def machines = viewMachine %:: super.machines

  def safeViewIO: Process[Task, view.FreeIO[_ <: View]] = {
    viewMachine.layout.discrete
      .headOr(dummyLayout)
  }

  def safeViewP: Process[Task, View] = {
    safeViewIO
      .map(_.perform())
      .sideEffect { v =>
        log.debug(s"setting view for $title:\n${v.viewTree.drawTree}")
      }
  }

  def safeView = {
    safeViewP
      .infraRunLastFor("obtain layout", 10 seconds)
      .getOrElse(dummyLayout.perform())
  }

  import iota.std.TextCombinators.text

  def dummyLayout = w[TextView] >>= text("Couldn't load content")
}

trait ASAAgentBase
extends RootAgent
{
  def safeViewIO: Process[Task, view.FreeIO[_ <: View]]

  def startP = {
    publish(Create(Map(), None))
  }

  def activityClass: Class[_ <: Activity] = classOf[StateAppViewActivity]
}

abstract class AppStateActivityAgent(implicit a: AndroidActivityUiContext,
  res: Resources)
extends ASAAgentBase
with ViewAgent
{
  implicit val activity = a.activity

  def title = "AppStateActivityAgent"

  def handle = "app_state_agent"
}

trait FreeViewAgent
extends Agent
with view.ExtViews
{
  def viewMachine: ViewMachine

  def title: String

  override def machines = viewMachine %:: super.machines

  import iota.std.TextCombinators.text

  def dummyLayout = w[TextView] >>= text("Couldn't load content")

  def safeViewIO: Process[Task, view.FreeIO[_ <: View]] = {
    viewMachine.layout.discrete
      .headOr(dummyLayout)
  }
}

trait ActAgent
extends ASAAgentBase
with FreeViewAgent
{
  def title = "ActAgent"

  def handle = "act_agent"
}
