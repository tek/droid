package tryp
package droid
package unit

import android.support.v7.widget.RecyclerView

import android.widget._

class NestedSpec
extends ActivitySpec[NestedActivity]
{
  def is = s2"""
  change text in a nested agent's view $nested
  """

  def before = ()

  def activityClass = classOf[NestedActivity]

  val text = Random.string(10)

  sequential

  def initialText = {
    activity.nested1.tv.text computes_== NestedActivity.text
  }

  def changedText = {
    activity.nestedAgent.send(NestedActivity.SetText(text))
    activity.nestedAgent.waitMachines()
    sync()
    activity.nestedAgent.nested2.tv.text computes_== text
  }

  def nested = {
    initialText and changedText
  }
}
