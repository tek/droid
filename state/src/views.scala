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
  override def handle = "io"

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

import AppState.{StartActivity, ActivityAgentStarted}

@Publish(StartActivity)
trait ActivityAgentBase
extends RootAgent

trait ActivityAgent
extends ActivityAgentBase
with ViewAgent { aa =>
  lazy val activityMachine = new Machine {
    override def handle = "activity"

    def admit: Admission = {
      case Create(_, _) => { s => s }
    }

    override def initialMessages = super.initialMessages ++
      emit(PublishMessage(ActivityAgentStarted(aa)))
  }

  override def machines = activityMachine %:: super.machines

  def activityClass: Class[_ <: Activity] = classOf[StateActivity]

  def title = "ActivityAgent"

  override def handle = "act_agent"
}
