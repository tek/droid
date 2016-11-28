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
