package tryp
package droid
package state

// FIXME exports Machine, clashing with tryp.state
// need to change export to pick the subclass's
@exportNames(StateApplication,
  MainViewAgent, ActivityAgent, IOMachine,
  IOTask, SimpleViewMachine, StateActivity)
trait Exports
extends view.Exports
{
  type ViewAgent[A <: ViewGroup] = tryp.droid.state.ViewAgent[A]
  type ViewMachine[A <: ViewGroup] = tryp.droid.state.ViewMachine[A]
  val MainViewMessages = tryp.droid.state.MainViewMessages
  val ViewAgent = tryp.droid.state.ViewAgent
  val IOOperation = tryp.droid.state.IOOperation
}

trait All
extends view.All
with ToViewStreamMessageOps
with StateEffectInstances
with IOEffect.ToIOEffectOps

@integrate(app, slick, slick.sync, view, tryp.state.StateDecls)
object `package`
extends All
{
  def Nop: Effect = tryp.state.Effect(Process.halt, "nop")
}
