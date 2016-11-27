package tryp
package droid
package integration

class ExtSpec
extends ExtIntStateSpec(classOf[IntStateActivity])
{
  override def initialUi = ViewAgent1.some

  def testExt() = {
    dbg("-------------")
    activity
    sleep(8)
    activity.showViewTree.dbg
  }
}
