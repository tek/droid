package tryp
package droid
package state

@exportNames(StateApplication, Nop, MainViewMessages, AppState, IOOperation,
  MainViewAgent, ActivityAgent, ViewAgent, Machine, Agent, ViewMachine,
  RootAgent, IOTask, SimpleViewMachine)
trait Exports
extends view.Exports

trait All
extends view.All
with StateEffect.ToStateEffectOps
with MiscEffectOps
with ToProcessSyntax
with TransitSyntax
with ToViewStreamMessageOps
with IOEffect.ToIOEffectOps

@integrate(view, state.core.StateDecls)
object `package`
extends All
{
  def Nop: Effect = Process.halt
}
