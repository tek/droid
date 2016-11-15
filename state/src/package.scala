package tryp
package droid
package state

// FIXME exports Machine, clashing with tryp.state
// need to change export to pick the subclass's
@exportNames(StateApplication,
  MainViewAgent, ActivityAgent, IOMachine,
  IOTask, SimpleViewMachine, StateActivity, ViewAgent, ViewMachine)
trait Exports
extends view.Exports
{
  type IOViewAgent[A <: ViewGroup] = tryp.droid.state.IOViewAgent[A]
  type IOViewMachine[A <: ViewGroup] = tryp.droid.state.IOViewMachine[A]
  val MainViewMessages = tryp.droid.state.MainViewMessages
  val ViewAgent = tryp.droid.state.ViewAgent
  val IOOperation = tryp.droid.state.IOOperation
  val Resume = tryp.droid.state.Resume
  val Update = tryp.droid.state.Update
  val Create = tryp.droid.state.Create
}

trait All
extends view.All
with ToViewStreamMessageOps
with StateEffectInstances
with IOEffect.ToIOEffectOps
with view.FragmentManagement.ToFragmentManagementOps
{
  def Nop: Effect = tryp.state.Effect(Process.halt, "nop")
}

@integrate(app, slick, slick.sync, view, tryp.state.StateDecls)
object `package`
extends All
