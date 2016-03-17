package tryp
package droid
package unit

import android.widget._

import state._

import shapeless._

import cats.data.Streaming

import iota.std.TextCombinators.text

trait NestedSpecMachine
extends SimpleViewMachine

object NestedActivity
{
  val text = Random.string(10)

  case class SetText(text: String)
  extends Message
}
import NestedActivity._

class NestedActivity
extends TestViewActivity
{
  val tk = text[TextView](NestedActivity.text)

  lazy val nested1 = new SimpleViewMachine
  {
    lazy val tv = w[TextView] >>= tk

    lazy val layoutIO = l[FrameLayout](tv :: HNil)
  }

  lazy val nestedAgent = new Agent {
    def handle = "nested_agent"

    lazy val nested2 = new SimpleViewMachine {
      lazy val tv = w[TextView] >>= tk

      lazy val layoutIO = l[FrameLayout](tv :: HNil)

      override def admit: Admission = {
        case SetText(content) => {
          case z =>
            z << (tv.v >>= text[TextView](content))
        }
      }
    }

    override def machines = nested2 %:: Streaming.Empty()
  }

  override def sub = Streaming.cons(nestedAgent, super.sub)

  override lazy val viewMachine =
    new NestedSpecMachine {
      lazy val layoutIO =
        l[FrameLayout](
          w[TextView] ::
          nested1.layoutIO ::
          nestedAgent.nested2.layoutIO ::
          HNil)
    }
}
