package tryp
package droid
package state

@exportTypes(StateApplication, StateActivity, AppState, ViewCell, ViewCellBase, ExtMVAppState, MainViewCell,
  DrawerViewCell)
trait Types

@exportVals(MVFrame, ExtMVFrame, DefaultScheduler, AndroidCell, StatePool)
trait Vals

@export
trait Exports
extends Types
with Vals

trait All
extends view.FragmentManagement.ToFragmentManagementOps

@integrate(view, tryp.state.core, tryp.state, tryp.state.ext, droid.state.core)
object `package`
extends All
{
  type AnyTree = iota.ViewTree[_ <: ViewGroup]
}
