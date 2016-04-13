package tryp
package droid
package unit

import state._

object ViewStreamSpec
{
  class Target(c: Context)
  extends TextView(c)

  class SpecAgent
  extends ActivityAgent
  {
    lazy val viewMachine =
      new SimpleViewMachine {
        lazy val search = w[Target]

        lazy val layout = l[FrameLayout](search)
      }
  }
}

class ViewStreamSpec
extends StateAppSpec
{
  import ViewStreamSpec._

  def is = s2"""
  ViewStream TextView

  access changed text through the signal $signal
  """

  val text = Random.string(10)

  override lazy val initialAgent = new SpecAgent

  def signal = {
    activity willContain view[Target] and {
      activity.viewOfType[Target] foreachA(_.setText(text))
      initialAgent.viewMachine.search.text computes_== text
    }
  }
}
