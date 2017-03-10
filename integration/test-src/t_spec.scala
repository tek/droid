package tryp
package droid
package integration

class BasicSpec
extends StateSpec[IntStateActivity](classOf[IntStateActivity])
{
  def testBasic() = {
    // dbg("-------------")
    activity
    sleep(5)
    // activity.showViewTree.dbg
  }
}
