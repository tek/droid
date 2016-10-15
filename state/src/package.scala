package tryp
package droid
package state

// FIXME exports Machine, clashing with tryp.state
// need to change export to pick the subclass's
@exportNames(StateApplication,
  MainViewAgent, ActivityAgent, ViewAgent, Machine, ViewMachine,
  IOTask, SimpleViewMachine)
trait Exports
extends view.Exports
{
  val MainViewMessages = tryp.droid.state.MainViewMessages
  val AppState = tryp.droid.state.AppState
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
  def Nop: Effect = Process.halt
}
