package tryp
package droid
package state

// FIXME exports Machine, clashing with tryp.state
// need to change export to pick the subclass's
@exportNames(StateApplication,
  MainViewAgent, ActivityAgent, IOTrans, IOTask, StateActivity, ViewAgent)
trait Exports
extends view.Exports
{
  // type IOViewAgent[A <: ViewGroup] = tryp.droid.state.IOViewAgent[A]
  // type IOViewMachine[A <: ViewGroup] = tryp.droid.state.IOViewMachine[A]
  val MainViewMessages = tryp.droid.state.MainViewMessages
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
  def Nop: Effect = tryp.state.Effect(Stream(), "nop")
}

// @integrate(app, slick, slick.sync, view, tryp.state.StateDecls, tryp.state)
@integrate(view, tryp.state.StateDecls, tryp.state)
object `package`
extends All
