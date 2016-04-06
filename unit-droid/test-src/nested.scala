package tryp
package droid
package unit

class NestedSpec
extends ActivitySpec[NestedActivity]
{
  def is = sequential ^ s2"""
  text in a nested agent's view

  show $show
  change $change
  """

  def activityClass = classOf[NestedActivity]

  val text = Random.string(10)

  def show = {
    activity.nested1.tv.text computes_== NestedActivity.text
  }

  def change = {
    activity.nestedAgent.publishOne(NestedActivity.SetText(text))
    sync()
    activity.nestedAgent.waitMachines()
    activity.nestedAgent.nested2.tv.text computes_== text
  }
}
