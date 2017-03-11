package tryp
package droid
package integration

class BasicSpec
extends StateSpec[IntStateActivity](classOf[IntStateActivity])
{
  def testBasic() = {
    activity
    sleep(1)
    val tv = for {
      ll <- activity.viewTree.subForest.headOption
      fl <- ll.subForest.lift(1)
      t <- fl.subForest.headOption
    } yield t.rootLabel
    val text = tv match {
      case Some(t: TextView) => t.getText
      case _ => ""
    }
    assert(text == "success")
  }
}
