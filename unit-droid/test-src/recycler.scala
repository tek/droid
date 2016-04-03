package tryp
package droid
package unit

import android.support.v7.widget.RecyclerView

import android.widget._

@frag(RecyclerSpecFragment)
class RecyclerSpec
{
  def is = s2"""
  empty $empty
  add $add
  """

  val text = Random.string(10)

  lazy val recFrag = frag[RecyclerSpecFragment]("test").getA

  def empty = {
    recFrag willContain emptyRecycler
  }

  def add = {
    recFrag.viewMachine.adapter.updateItems(List("first", "second")).run
    recFrag willContain nonEmptyRecycler(2)
  }
}

class RecyclerActivitySpec
extends ActivitySpec[RecyclerActivity]
{
  def is = s2"""
  empty $empty
  add $add
  """

  def activityClass = classOf[RecyclerActivity]

  val text = Random.string(10)

  sequential

  def empty = {
    activity willContain emptyRecycler
  }

  def add = {
    activity.viewMachine.adapter.updateItems(List("first", "second")).run
    activity willContain nonEmptyRecycler(2)
  }
}
