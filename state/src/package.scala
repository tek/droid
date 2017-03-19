package tryp
package droid
package state

@exportTypes(StateApplication, AndroidMachine, StateActivity, AppState)
trait Types
{
  type ViewMachine[A <: tryp.droid.state.ViewMachineTypes.AnyTree] = tryp.droid.state.ViewMachine[A]
}

@exportVals(MVFrame)
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
