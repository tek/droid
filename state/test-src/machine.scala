package tryp
package droid
package state

import org.specs2._, specification._, matcher._, concurrent._

import scalaz._, Scalaz._, scalaz.concurrent._, stream.async

import shapeless.HNil

import droid.state._

object AgentSpecData
{
  case object Go
  extends Message

  case object Step1
  extends Message

  case object Step2
  extends Message

  case object Step3
  extends Message

  case object S1
  extends BasicState

  case object S2
  extends BasicState

  case object S3
  extends BasicState

  case object S4
  extends BasicState
}
import AgentSpecData._

@Publish(Step3)
trait MachineSpecMachine
extends Machine
{
  def handle = "state1"

  lazy val output = async.signalOf(-1)

  def admit: Admission = {
    case Go ⇒ {
      case S(S1, d) ⇒
        S(S2, d) << output.set(1) << Step1
    }
    case Step1 ⇒ {
      case s @ S(S2, d) ⇒
        S(S3, d) << output.set(0) << Step2
    }
  }

  override def stateAdmit: StateAdmission = {
    case s @ S(S3, d) ⇒ {
      case Step2 ⇒
        S(S4, d) << Step3 << QuitMachine
    }
  }
}

trait MachineSpecBase
extends Spec
with tryp.Matchers
{
  def is = s2"""
  reach state S4 $endState
  produce message $message
  set the signal to 0 $signal
  """

  lazy val state = new MachineSpecMachine {}

  def fsm: MProc

  def message = fsm will_== Step3

  def endState = state.current.get will_== Zthulhu(S4, NoData)

  def signal = state.output.get will_== 0
}

class MachineSpec
extends MachineSpecBase
{
  lazy val fsm = {
    state.send(Go)
    state.runWithInitial(Zthulhu(state = S1))
  }
}

