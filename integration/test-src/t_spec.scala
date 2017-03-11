package tryp
package droid
package integration

class BasicSpec
extends StateSpec[IntStateActivity](classOf[IntStateActivity])
{
  def testBasic() = {
    activity
    sleep(5)
  }
}
