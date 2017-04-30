package tryp
package droid
package integration

class BasicSpec
extends StateSpec[IntStateActivity](classOf[IntStateActivity])
{
  def intAppState = activity.stateApp.state match {
    case a: IntAppState => a
    case _ => sys.error("no IntApp")
  }

  def mainLayout = intAppState.mainView.mainView match {
    case Some(a) => a
    case _ => sys.error("no main view")
  }

  def mainFrame = mainLayout.mainFrame

  def showTree(tree: String) = log.info("\n" + tree)

  def showWindow = showTree(activity.showViewTree)

  def testBasic() = {
    activity
    sleep(1)
    val tv = for {
      fl <- mainFrame.viewTree.subForest.headOption
      t <- fl.subForest.lift(0)
    } yield t.rootLabel
    val text = tv match {
      case Some(t: TextView) => t.getText
      case a => a.toString
    }
    showTree(mainFrame.showViewTree)
    assert(text == "success")
  }
}
