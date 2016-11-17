package tryp
package droid
package unit

import tryp.state._

import org.specs2._, specification._, matcher._, concurrent._

import scalaz._, Scalaz._, scalaz.concurrent._, stream._

import shapeless.HNil

case object Go
extends Message

case object Received0
extends Message

case object Received1
extends Message

case object Received2
extends Message

case object Received3
extends Message

case object Set0
extends Message

case object Set1
extends Message

trait State0
extends Machine
{
  override def handle = "state0"

  def admit: Admission = {
    case Received3 => {
      case s => s << Received0.publish
    }
  }
}

@Publish(Received1)
trait State1
extends Machine
{
  override def handle = "state1"

  lazy val output = async.signalOf(-1)

  def admit: Admission = {
    case Go => {
      case s => s << Received1
    }
    case Received0 => {
      case s => s << output.set(0) << Set0
    }
    case Set1 => {
      case s => s << output.set(1)
    }
  }
}

trait State2
extends Machine
{
  override def handle = "state2"

  def admit: Admission = {
    case Received1 => {
      case s => s << Received2.toLocal
    }
    case Set0 => {
      case s => s << Set1
    }
  }
}

@Publish(Received3)
trait State3
extends Machine
{
  override def handle = "state3"

  def admit: Admission = {
    case Received2 => {
      case s => s << Received3 << Set1.toAgentMachine
    }
  }
}

class AgentSpec
extends tryp.Spec
with FixedPool
{
  val name = "foo"
  val threads = 10

  def is = s2"""
  machine $machine
  """

  lazy val root = new RootAgent {
      val ag1 = new Agent {
        override def handle = "ag1"

        lazy val state = new State1 {}

        override def machines = state :: super.machines
      }

      val ag2 = new Agent {
        override def handle = "ag2"

        lazy val output = async.signalOf(-1)

        lazy val state2 = new State2 {}

        lazy val state3 = new State3 {}

        override def machines = state2 :: state3 :: super.machines

        override def admit: Admission = {
          case Set1 => {
            case s => s << output.set(1)
          }
        }
      }

      override def handle = "root"

      lazy val state = new State0 {}

      override def machines = state :: super.machines

      override def sub = ag1 :: ag2 :: super.sub
  }

  def machine = {
    root.runAgent()
    root.ag1.schedule1(Go.publish)
    (root.ag2.output.get will_== 1) and (root.ag1.state.output.get will_== 0)
  }
}
