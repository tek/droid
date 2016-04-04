package tryp
package droid
package trial

import view.core._
import view._
import state.core._
import state._
import state._
import io.recycler._

import android.support.v7.widget.RecyclerView
import android.widget._

trait MainViewMachine
extends CKCombinators[RecyclerView, StreamIO]
with RecyclerViewMachine[StringRecyclerAdapter]
{
  def recyclerConf = nopK
}

class MainActivity2
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
