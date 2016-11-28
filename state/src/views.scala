package tryp
package droid
package state

import android.widget._

import tryp.state.PublishMessage

case class IODispatcher(mcomm: MComm)
extends MachineTransitions
{
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
    // case t @ ViewStreamTask(_, _, _, _) =>
    //   _ << t.effect
  }
}

trait IODispatcherMachine
extends Machine
{
  def transitions(c: MComm) = IODispatcher(c)
}

import AppState.ActivityAgentStarted

trait ActivityAgentBase
extends Agent

trait ActivityAgent
extends ActivityAgentBase { aa =>
  def activityClass: Class[_ <: Activity] = classOf[StateActivity]

  override def initialMessages =
    super.initialMessages ++ Stream(PublishMessage(ActivityAgentStarted(aa)))
}

trait TreeActivityAgent
extends ActivityAgent
with ViewAgent
