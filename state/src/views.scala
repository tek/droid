package tryp
package droid
package state

import scalaz._, Scalaz._, concurrent._, stream._, Process._
import android.widget._

import core._
import view._
import view.core._

trait IODispatcher
extends Machine
{
  def handle = "io"

  val admit: Admission = {
    case t: IOTask[_, _, _] => {
      case s =>
        s << t.effect
    }
    case t @ IOFork(io, timeout) => {
      case s => s << t.effect
    }
    case t @ IOMainTask(io, timeout) => {
      case s => s << t.effect
    }
    case t @ ViewStreamTask(stream, timeout, main) =>
      _ << t.effect
  }
}

trait DummyViewMachine
extends ViewMachine
{
  def layoutIO = w[View]
}

trait ViewAgent
extends Agent
with Views[Context, StreamIO]
{
  def viewMachine: ViewMachine

  override def machines = viewMachine %:: super.machines

  import iota.std.TextCombinators.text

  def dummyLayout: StreamIO[View, Context] =
    (w[TextView] >>= text("Couldn't load content")) map(a => a: View)

  def viewIO = viewMachine.layout.discrete.take(1)

  def safeViewIO: Process[Task, StreamIO[View, Context]] = {
    viewIO.availableOrHalt
  }
}

trait FreeViewAgent
extends ViewAgent
{
  implicit def activity: Activity

  def title: String

  def safeViewP: Process[Task, View] = {
    safeViewIO
      .map(_.unsafePerformIO)
      .eval
      .sideEffect { v =>
        log.debug(s"setting view for $title:\n${v.viewTree.drawTree}")
      }
  }

  def safeView: View = {
    safeViewP
      .infraRunLastFor("obtain layout", 10 seconds)
      .getOrElse(dummyLayout.unsafePerformIO.unsafePerformSync)
  }
}

import AppState.{StartActivity, ActivityAgentStarted}

@Publish(StartActivity)
trait ActivityAgentBase
extends RootAgent

trait ActivityAgent
extends ActivityAgentBase
with ViewAgent { aa =>
  lazy val activityMachine = new Machine {
    def handle = "activity"

    def admit: Admission = {
      case Create(_, _) => { s => s }
    }

    override def initialMessages = super.initialMessages ++
      emit(PublishMessage(ActivityAgentStarted(aa)))
  }

  override def machines = activityMachine %:: super.machines

  def activityClass: Class[_ <: Activity] = classOf[StateAppActivity]

  def title = "ActivityAgent"

  def handle = "act_agent"
}
