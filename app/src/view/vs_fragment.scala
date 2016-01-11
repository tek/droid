package tryp
package droid

import android.support.v7.widget.RecyclerView

import iota._

import state._

trait RecyclerViewMachine[A <: RecyclerView.Adapter[_]]
extends SimpleViewMachine[RecyclerView]
{
  def adapter: A

  def recyclerConf: CK[RecyclerView]

  def recycler = layoutIO

  def layoutIO = w[RecyclerView] >>= recyclerAdapter(adapter) >>= recyclerConf

  def update() { Ui(adapter.notifyDataSetChanged()).run }
}

trait RecyclerVSFragment[A <: RecyclerView.Adapter[_]]
extends VSTrypFragment
{ outer ⇒

}
