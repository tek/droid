package tryp
package droid

import state._
import state.core._
import view._
import view.core._

import android.support.v7.widget.RecyclerView

import shapeless._

import iota._

trait RecyclerViewMachine[A <: RecyclerView.Adapter[_]]
extends ViewMachine
{
  import io.recycler._

  def adapter: A

  def recyclerConf: CK[RecyclerView, StreamIO]

  def recycler =
    w[RecyclerView] >>-
      recyclerAdapter(adapter) >>-
      recyclerConf >>-
      recyclerLayout

  def recyclerLayout: CK[RecyclerView, StreamIO] = linear

  def layoutIO = l[FrameLayout](recycler)

  // def update() { Ui(adapter.notifyDataSetChanged()).run }
}

trait RecyclerVSFragment[A <: RecyclerView.Adapter[_]]
extends VSTrypFragment
{ outer =>

}
