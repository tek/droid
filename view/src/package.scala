package tryp
package droid
package view

@exportNames(
  StreamIO, ViewStream, RootView,
  android.support.v7.widget.RecyclerView,
  android.support.v7.widget.Toolbar
)
trait Names
{
  val StreamIO = droid.view.StreamIO
}

@export
trait Exports
extends tryp.state.Exports
with view.core.Exports
with Names
{
  type RecyclerViewHolder = android.support.v7.widget.RecyclerView.ViewHolder
  type RecyclerViewAdapter[A <: RecyclerViewHolder] =
    android.support.v7.widget.RecyclerView.Adapter[A]
}

trait All
extends state.All
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
with BuilderOps

@integrate(tryp.state, view.core)
object `package`
extends All

package object io
extends All
with Names
