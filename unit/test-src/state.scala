package tryp
package droid
package unit

import org.specs2._, specification._, matcher._, concurrent._

import scalaz._, Scalaz._, scalaz.concurrent._, stream.async

import droid.state._

trait Matchers
extends SpecificationLike
{
  def become[A](target: A): Matcher[Task[A]] = { (ta: Task[A]) ⇒
    ta.attemptRun.toOption must beSome(target)
  } eventually
}

case object Go
extends Message

case object Received0
extends Message

case object Received1
extends Message

case object Received2
extends Message


trait State0
extends Machine[HNil]
{
  def handle = "state0"

  def admit: Admission = {
    case Received2 ⇒ {
      case s ⇒ s  << Received0
    }
  }
}

trait State1
extends Machine[HNil]
{
  def handle = "state1"

  lazy val output = async.signalOf(false)

  def admit: Admission = {
    case Go ⇒ {
      case s ⇒ s << Received1
    }
    case Received0 ⇒ {
      case s ⇒ s << output.set(true)
    }
  }
}

trait State2
extends Machine[HNil]
{
  def handle = "state2"

  def admit: Admission = {
    case Received1 ⇒ {
      case s ⇒ s << Received2
    }
  }
}

class MachineSpec
extends Specification
with Matchers
{
  def is = s2"""
  machine $machine
  """

  def machine = {
    val med = new Mediator {
      lazy val state = new State0 {}

      override def machines = state :: super.machines
    }
    val ag1 = new Agent {
      lazy val state = new State1 {}

      override def machines = state :: super.machines

      val mediator = med
    }
    val ag2 = new Agent {
      lazy val state = new State2 {}

      override def machines = state :: super.machines

      val mediator = med
    }
    med.initMachines()
    ag1.initMachines()
    ag2.initMachines()
    ag1.send(Go)
    ag1.state.output.get must become(true)
  }
}
