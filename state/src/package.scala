package tryp
package droid
package state

@exportTypes(StateApplication, StateActivity, AppState, ViewCell, ViewCellBase, ExtMVAppState, MainViewCell)
trait Types

@exportVals(MVFrame, ExtMVFrame, DefaultScheduler, ViewCellData, AndroidCell)
trait Vals

@export
trait Exports
extends Types
with Vals

trait All

@integrate(view, tryp.state.core, tryp.state, droid.state.core)
object `package`
extends view.FragmentManagement.ToFragmentManagementOps
{
  type AnyTree = iota.ViewTree[_ <: ViewGroup]
}
