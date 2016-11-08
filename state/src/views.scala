package tryp
package droid
package state

import tryp.state._

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
    case t @ IOMainTask(_, _, _) => {
      case s => s << t.effect
    }
    case t @ ViewStreamTask(_, _, _, _) =>
      _ << t.effect
  }
}

import AppState.{StartActivity, ActivityAgentStarted}

@Publish(StartActivity)
trait ActivityAgentBase
extends Agent

trait ActivityAgent
extends ActivityAgentBase { aa =>
  lazy val activityMachine = new Machine {
    override def handle = "activity"

    def admit: Admission = {
      case Create(_, _) => { s => s }
    }

    override def initialMessages = super.initialMessages ++
      emit(PublishMessage(ActivityAgentStarted(aa)))
  }

  override def machines = activityMachine :: super.machines

  def activityClass: Class[_ <: Activity] = classOf[StateActivity]

  def title = "ActivityAgent"

  override def handle = "act_agent"
}

trait IOActivityAgent
extends ActivityAgent
with IOViewAgent[ViewGroup]

trait TreeActivityAgent
extends ActivityAgent
with ViewAgent

object ActivityAgent
{
  def apply(lay: StreamIO[ViewGroup, Context]) =
    new ActivityAgent {
      lazy val viewMachine = ViewMachine(lay)
    }
}
