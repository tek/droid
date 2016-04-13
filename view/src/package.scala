package tryp
package droid
package view

@exportNames(
  StreamIO, ViewStream,
  android.support.v7.widget.RecyclerView
)
trait Exports
extends view.core.Exports
{
  type RecyclerViewHolder = android.support.v7.widget.RecyclerView.ViewHolder
  type RecyclerViewAdapter[A <: RecyclerViewHolder] =
    android.support.v7.widget.RecyclerView.Adapter[A]
}

trait All
extends state.core.All
with view.core.All
with RootView.ToRootViewOps
with ToSearchable
with ToSearchView
with HasContextF.ToHasContextFOps
with HasActivityF.ToHasActivityFOps
with ViewInstances
with ToViewOps
with ToViewGroupOps
with StartActivity.ToStartActivityOps
with StartActivityForResult.ToStartActivityForResultOps
with Auth.ToAuthOps
with TOIOProcess

@integrate(state.core, state.core.Names)
object `package`
extends All

package object io
extends All
