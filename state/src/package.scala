package tryp
package droid
package state

@exportTypes(StateApplication, StateApplicationAgent, MainViewAgent,
  ActivityAgent, IOTrans, ViewMachine)
trait Types

@exportVals(MainViewMessages, AppState, IOOperation, Resume, Update, Create,
  DefaultScheduler)
trait Vals

@exportNames(IOTask, StateActivity, ViewAgent)
trait Exports
extends view.Exports
with Vals
with Types

trait All
extends view.All
with ToViewStreamMessageOps
with StateEffectInstances
with IOEffect.ToIOEffectOps
with view.FragmentManagement.ToFragmentManagementOps
{
  def Nop: Effect = tryp.state.Effect(Stream(), "nop")
}

@integrate(view, tryp.state)
object `package`
extends All
