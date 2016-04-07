package tryp
package droid
package unit

import cats.data.Streaming

import state.core._
import state._
import view._

object NestedSpec
{
  val oldText = Random.string(10)

  case class SetText(text: String)
  extends Message

  class NestedAgent
  extends ActivityAgent
  {
    val tk = io.text.text(oldText)

    lazy val nested1 = new ViewMachine
    {
      lazy val tv = w[TextView] >>- tk

      lazy val layoutIO = l[FrameLayout](tv)
    }

    lazy val nestedAgent = new Agent {
      override def handle = "nested_agent"

      lazy val nested2 = new ViewMachine {
        lazy val tv = w[TextView] >>- tk

        lazy val layoutIO = l[FrameLayout](tv)

        override def admit: Admission = {
          case SetText(content) =>
            _ << (tv.v >>- io.text.text(content)).void.main
        }
      }

      override def machines = nested2 %:: super.machines
    }

    override def machines = nested1 %:: super.machines

    override def sub = nestedAgent %:: super.sub

    override lazy val viewMachine =
      new ViewMachine {
        lazy val layoutIO =
          l[FrameLayout](
            w[TextView],
            nested1.layoutIO,
            nestedAgent.nested2.layoutIO
          )
      }
  }
}

class NestedSpec
extends StateAppSpec
{
  import NestedSpec._

  def is = sequential ^ s2"""
  text in a nested agent's view

  show $show
  change $change
  """

  override def retries = 3

  override lazy val initialAgent = new NestedAgent

  val newText = Random.string(10)

  def show = {
    activity willContain text(oldText) and (
      initialAgent.nested1.tv.text computes_== oldText) and {
        initialAgent.nestedAgent.publishOne(SetText(newText))
        1 === 1
      }
  }

  def change = {
    activity willContain text(newText) and (
      initialAgent.nestedAgent.nested2.tv.text computes_== newText)
  }
}
