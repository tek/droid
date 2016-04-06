package tryp
package droid
package view

trait ExportDecls
{
  type RecyclerView = android.support.v7.widget.RecyclerView
  type RecyclerViewHolder = android.support.v7.widget.RecyclerView.ViewHolder
  type RecyclerViewAdapter[A <: RecyclerViewHolder] =
    android.support.v7.widget.RecyclerView.Adapter[A]
}

trait Exports
extends view.core.Exports
with state.core.Exports
with state.core.ExportDecls
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

object `package`
extends Exports

package object io
extends Exports
