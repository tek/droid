package tryp
package droid
package unit

import org.specs2._, specification._, matcher._, concurrent._

import scalaz._, Scalaz._, scalaz.concurrent._, stream.async

import shapeless.HNil

import droid.state._

case object Go
extends Message

case object Received0
extends Message

case object Received1
extends Message

case object Received2
extends Message

case object Set0
extends Message

case object Set1
extends Message

@Publish(Received0)
trait State0
extends Machine
{
  def handle = "state0"

  def admit: Admission = {
    case Received2 => {
      case s => s  << Received0
    }
  }
}

@Publish(Received1)
trait State1
extends Machine
{
  def handle = "state1"

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

@Publish(Received2)
trait State2
extends Machine
{
  def handle = "state2"

  def admit: Admission = {
    case Received1 => {
      case s => s << Received2
    }
    case Set0 => {
      case s => s << Set1
    }
  }
}

class MediationSpec
extends Specification
with tryp.Matchers
{
  def is = s2"""
  machine $machine
  """

  def machine = {
    val root = new RootAgent
    {
      med =>

        val ag1 = new Agent {
          def handle = "ag1"

          lazy val state = new State1 {}

          override def machines = state %:: super.machines
        }

        val ag2: Agent = new Agent {
          def handle = "ag2"

          lazy val state = new State2 {}

          override def machines = state %:: super.machines
        }

        def handle = "med"

        lazy val state = new State0 {}

        override def machines = state %:: super.machines

        override def sub = ag1 %:: ag2 %:: super.sub
    }
    root.runAgent()
    root.ag1.send(Go)
    root.ag1.state.output.get will_== 0
  }
}
