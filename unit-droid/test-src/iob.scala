package tryp
package droid
package unit

class IOBSpec
extends SpecBase
{
  def is = s2"""
  signal $signal
  """

  def before = {
    activity
    // activity.navigateIndex(0)
  }

  def signal = {
    activity
    sync()
    Thread.sleep(2000)
    sync()
    pr(activity.viewTree.drawTree)
    frag[SpecFragment]("test") foreachA { f ⇒
    }
    1 === 1
  }
}
