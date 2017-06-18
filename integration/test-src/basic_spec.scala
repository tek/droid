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
    unsafeSend(UpdateInt(strings))
    sleep(1)
    val tv = for {
      fl <- mainFrame.viewTree.sub.headOption
      rv <- fl.sub.headOption
      tf1 <- rv.sub.headOption
      tf2 <- rv.sub.lift(1)
      t1 <- tf1.sub.headOption
      t2 <- tf2.sub.headOption
    } yield List(t1.data, t2.data)
    val labels = tv match {
      case Some(l @ List(t1: TextView, t2: TextView)) => List(t1, t2).map(_.getText)
      case a => a.toString
    }
    showTree(mainFrame.showViewTree)
    labels must_== strings
  }
}
