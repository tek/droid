package tryp
package droid
package integration

class BasicSpec
extends SimpleIntStateSpec(classOf[IntStateActivity])
{
  override def initialUi = ViewAgent1.some

  def testBasic() = {
    dbg("-------------")
    activity
    sleep(8)
    activity.showViewTree.dbg
  }
}
