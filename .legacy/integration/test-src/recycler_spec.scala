package tryp
package droid
package integration

class RecyclerSpec
extends SimpleIntStateSpec
{
  override def initialUi = RecyclerAgent.some

  def testRecycler() = {
    dbg("-------------")
    activity.nonEmptyRecycler(10)
    activity.showViewTree.dbg
  }
}

class ExtRecyclerSpec
extends ExtIntStateSpec
{
  override def initialUi = RecyclerAgent.some

  def testExt() = {
    Logging.debugTryp()
    dbg("-------------")
    activity.nonEmptyRecycler(20)
    activity.showViewTree.dbg
  }
}
