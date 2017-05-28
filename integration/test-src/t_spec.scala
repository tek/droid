package tryp
package droid
package integration

class BasicSpec
extends Spec
{
  def is = s2"""
  basic $basic
  """

  def basic = {
    sleep(1)
    val strings = List("first", "second")
    send(UpdateInt(strings))
    sleep(1)
    val tv = for {
      fl <- mainFrame.viewTree.subForest.headOption
      rv <- fl.subForest.headOption
      tf1 <- rv.subForest.headOption
      tf2 <- rv.subForest.lift(1)
      t1 <- tf1.subForest.headOption
      t2 <- tf2.subForest.headOption
    } yield List(t1.rootLabel, t2.rootLabel)
    val labels = tv match {
      case Some(l @ List(t1: TextView, t2: TextView)) => List(t1, t2).map(_.getText)
      case a => a.toString
    }
    showTree(mainFrame.showViewTree)
    labels must_== strings
  }
}
