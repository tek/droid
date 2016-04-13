package tryp
package droid
package state

@export
trait Exports
extends state.core.Exports
with state.core.Names
with state.core.StateDecls
{
  def Nop: Effect = Process.halt
}

trait All
extends StateEffect.ToStateEffectOps
with MiscEffectOps
with ToProcessSyntax
with TransitSyntax
with ToViewStreamMessageOps
with IOEffect.ToIOEffectOps

@integrate(view, state.core, state.core.Names, state.core.StateDecls)
object `package`
extends Exports
with All
with droid.core.All
with state.core.All
with view.core.All
