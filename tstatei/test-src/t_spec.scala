package tryp
package droid
package tstatei

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
