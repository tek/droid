package tryp
package droid
package integration

class ExtSpec
extends ExtIntStateSpec
{
  override def initialUi = ViewAgent1.some

  def testExt() = {
    dbg("-------------")
    activity
    sleep(8)
    activity.showViewTree.dbg
  }
}
