package tryp
package droid
package integration

class RecyclerSpec
extends SimpleIntStateSpec(classOf[IntStateActivity])
{
  override def initialUi = RecyclerAgent.some

  def testRecycler() = {
    dbg("-------------")
    activity
    activity.nonEmptyRecycler(10)
    activity.showViewTree.dbg
  }
}
