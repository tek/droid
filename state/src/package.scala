package tryp
package droid
package state

trait ExportDecls
{
  def Nop: Effect = Process.halt
}

trait Exports
extends state.core.Exports
with StateEffect.ToStateEffectOps
with ToStateEffect
with MiscEffectOps
with ToProcessSyntax
with TransitSyntax
with ToViewStreamMessageOps
with IOEffect.ToIOEffectOps
with view.ExportDecls

object `package`
extends Exports
with ExportDecls
with state.core.ExportDecls
with state.core.StateDecls
with view.core.Exports
