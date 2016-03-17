package tryp
package droid

import android.support.v7.widget.RecyclerView

import shapeless._

import iota._

import state._

trait RecyclerViewMachine[A <: RecyclerView.Adapter[_]]
extends SimpleViewMachine
{
  import RecyclerCombinators._

  def adapter: A

  def recyclerConf: CK[RecyclerView]

  def recycler =
    w[RecyclerView] >>-
      recyclerAdapter(adapter) >>-
      recyclerConf >>-
      recyclerLayout

  def recyclerLayout: CK[RecyclerView] = linear

  def layoutIO = l[FrameLayout](recycler :: HNil)

  def update() { Ui(adapter.notifyDataSetChanged()).run }
}

trait RecyclerVSFragment[A <: RecyclerView.Adapter[_]]
extends VSTrypFragment
{ outer =>

}
