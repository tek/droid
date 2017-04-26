package tryp
package droid
package state

@exportTypes(StateApplication, AndroidCell, StateActivity, AppState)
trait Types
{
  type ViewCell[A <: tryp.droid.state.ViewCellTypes.AnyTree] = tryp.droid.state.ViewCell[A]
}

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
// with IOEffect.ToIOEffectOps
with IOParcel
