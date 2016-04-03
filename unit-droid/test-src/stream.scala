package tryp
package droid
package unit

import scalaz._, Scalaz._, concurrent.Task

import android.widget._

class ViewStreamSpec
extends SpecBase
with DefaultStrategy
{
  def is = s2"""
  signal $signal
  """

  val text = Random.string(10)

  def signal = {
    val f = frag[SpecFragment]("test").getA
    f.viewOfType[TextView] foreachA(_.setText(text))
    f.viewMachine.search.text computes_== text
  }
}
