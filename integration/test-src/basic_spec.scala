package tryp
package droid
package integration

class BasicSpec
extends StateSpec(classOf[IntStateActivity])
with Logging
{
  def testBasic() = {
    dbg("-------------")
    activity
    sleep(8)
    activity.showViewTree.dbg
  }
}
