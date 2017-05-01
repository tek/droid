package tryp
package droid
package state

@exportTypes(StateApplication, AndroidCell, StateActivity, AppState, ViewCell)
trait Types

@exportVals(MVFrame, ExtMVFrame)
trait Vals

@export
trait Exports
extends Types
with Vals

trait All
extends IOParcel

@integrate(view, tryp.state.core, tryp.state, droid.state.core)
object `package`
extends view.FragmentManagement.ToFragmentManagementOps
with IOParcel
{
  type AnyTree = iota.ViewTree[_ <: ViewGroup]
}
