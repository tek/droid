package tryp
package droid
package trial

import view._
import state._

import android.support.v7.widget.RecyclerView

trait MainViewMachine
extends RecyclerViewMachine[StringRecyclerAdapter]
with RecyclerCombinators
{
  def recyclerConf = nopK
}

class MainActivity
extends Activity
with ViewActivity
{
  override lazy val viewMachine =
    new MainViewMachine {
      lazy val adapter = new StringRecyclerAdapter {}
    }

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    viewMachine.adapter.updateItems(List("first", "second")).run
  }
}
