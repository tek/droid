package tryp
package droid
package unit

import android.support.v7.widget.RecyclerView

trait RecyclerSpecMachine
extends RecyclerViewMachine[StringRecyclerAdapter]
with RecyclerCombinators
{
  def recyclerConf = nopK
}

case class RecyclerSpecFragment()
extends RecyclerVSFragment[StringRecyclerAdapter]
{
  lazy val viewMachine: RecyclerSpecMachine = new RecyclerSpecMachine {
    def adapter = new StringRecyclerAdapter {}
  }
}

class RecyclerActivity
extends TestViewActivity
{
  override lazy val viewMachine =
    new RecyclerSpecMachine {
      lazy val adapter = new StringRecyclerAdapter {}
    }
}
