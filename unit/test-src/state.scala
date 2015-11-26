package tryp
package droid
package unit

import org.specs2._

import state._

class DummyAndroidUiContext
extends AndroidUiContext
{
  def loadFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def transitionFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def showViewTree(view: View) = "not implemented"

  def notify(id: String): Ui[Any] = Ui("asdf")
}

trait State0
extends Machine[HNil]
{
  def handle = "state0"

  def transitions: ViewTransitions = {
    case _ ⇒ { case s ⇒ s }
  }
}

trait State1
extends Machine[HNil]
{
  def handle = "state1"

  def transitions: ViewTransitions = {
    case _ ⇒ { case s ⇒ s }
  }
}

trait State2
extends Machine[HNil]
{
  def handle = "state2"

  def transitions: ViewTransitions = {
    case _ ⇒ { case s ⇒ s }
  }
}

class MachineSpec
extends Specification
{
  def is = s2"""
  machine $machine
  """

  def machine = {
    implicit val c = new DummyAndroidUiContext
    val med = new Mediator {
      lazy val state = new State0 {}

      override def machines = state :: super.machines
    }
    val ag1 = new Agent {
      lazy val state = new State1 {}

      override def machines = state :: super.machines
    }
    val ag2 = new Agent {
      lazy val state = new State2 {}

      override def machines = state :: super.machines
    }
    1 === 1
  }
}
