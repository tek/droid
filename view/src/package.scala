package tryp
package droid
package view

@exportTypes(RootView, android.support.v7.widget.RecyclerView, android.support.v7.widget.Toolbar, AnnotatedAIO)
trait Types

@export
trait Exports
extends view.core.Exports
with Types
// with Names
{
  type RecyclerViewHolder = android.support.v7.widget.RecyclerView.ViewHolder
  type RecyclerViewAdapter[A <: RecyclerViewHolder] =
    android.support.v7.widget.RecyclerView.Adapter[A]
}

trait All
extends RootView.ToRootViewOps
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
with BuilderOps
with ViewEq

@integrate(view.core)
object `package`
extends All

package object io
extends All
with Types
// with Names
