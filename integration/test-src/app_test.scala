package tryp
package droid
package integration

class AppSpec
extends StateSpec(classOf[IntStateActivity])
with Logging
{
  def testSomething() = {
    log.error("asdf")
    dbg("-------------")
    activity
    sleep(3)
    activity.showViewTree.dbg
  }
}
