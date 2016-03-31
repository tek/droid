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
    pr(sys.allThreads)
    case class Muto(var state: Int)
    val v1 = 9
    val v2 = 47
    val kest = { (a: Muto) => a.state = v2; Muto(v1 + v2) }
    import concurrent.ExecutionContext.Implicits.global
    val s = StreamIO.lift[Int](v => Muto(v)) >>- kest >>- ((_: Muto) => pr(Thread.currentThread))
    iota.IO(1).performMain()
    Thread.sleep(1000)
    pr(sys.allThreads)
    s.main()(v1) will_== Muto(v2)
  }
}
