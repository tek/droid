package tryp
package droid
package unit

import state._
import state.core._
import view._
import view.core._

import android.support.v7.widget.RecyclerView

import io.recycler._

trait RecyclerSpecMachine
extends CKCombinators[RecyclerView, StreamIO]
with RecyclerViewMachine[StringRecyclerAdapter]
{
  def recyclerConf = nopK
}

case class RecyclerSpecFragment()
extends RecyclerVSFragment[StringRecyclerAdapter]
{
  def title = "recycler spec fragment"

  def handle = "rec_frag"

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
