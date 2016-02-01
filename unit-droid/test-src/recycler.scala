package tryp
package droid
package unit

import android.support.v7.widget.RecyclerView

import android.widget._

@frag(RecyclerSpecFragment)
class RecyclerSpec
{
  def is = s2"""
  add $add
  """

  def before = ()

  val text = Random.string(10)

  def add = {
    Thread.sleep(3000)
    val f = frag[RecyclerSpecFragment]("test").getA
    f.viewMachine.adapter.updateItems(List("first", "second"))
    Thread.sleep(3000)
    f.nonEmptyRecycler(1)
    1 === 1
  }
}


class RecyclerActSpec
extends ActivitySpec[ThinSpecActivity]
{
  def is = s2"""
  add $add
  """

  def before = ()

  def activityClass = classOf[ThinSpecActivity]

  val text = Random.string(10)

  def add = {
    activity
    activity.viewMachine.adapter.updateItems(List("first", "second")).run
    sync()
    activity.nonEmptyRecycler(2)
    1 === 1
  }
}
