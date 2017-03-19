package tryp
package droid
package integration

class BasicSpec
extends StateSpec[IntStateActivity](classOf[IntStateActivity])
{
  def testBasic() = {
    activity
    sleep(3)
    activity.showViewTree.dbg
    val tv = for {
      ll <- activity.viewTree.subForest.headOption
      fl1 <- ll.subForest.lift(1)
      mf <- fl1.subForest.headOption
      fl2 <- mf.subForest.headOption
      t <- fl2.subForest.headOption
    } yield t.rootLabel
    val text = tv match {
      case Some(t: TextView) => t.getText
      case _ => ""
    }
    assert(text == "success")
  }
}
