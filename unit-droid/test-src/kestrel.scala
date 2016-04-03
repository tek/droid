package tryp
package droid
package unit

import view._

class PerformCKMainSpec
extends UnitSpecs2Spec[Activity]
{
  def is = s2"""
  test $test
  """

  def before = ()

  def activityClass = classOf[Activity]

  def test = {
    case class Muto(var state: Int)
    val v1 = 9
    val v2 = 47
    val kest = { (a: Muto) => a.state = v2; Muto(v1 + v2) }
    val s = StreamIO.lift[Int](v => Muto(v)) >>- kest
    s.main()(v1) will_== Muto(v2)
  }
}
