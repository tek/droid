package tryp
package droid
package integration

class BasicSpec
extends SimpleIntStateSpec
{
  override def initialUi = ViewAgent1.some

  def testBasic() = {
    dbg("-------------")
    activity
    sleep(8)
    activity.showViewTree.dbg
  }
}
