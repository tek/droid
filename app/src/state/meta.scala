package tryp
package droid
package state

trait Exports
extends MiscEffectOps
with ToOperationSyntax
with ToProcessSyntax
with ToStateEffectSyntax
{
  val Nop: Effect = Process.halt

  type Agent = droid.state.Agent
  type SolitaryAgent = droid.state.SolitaryAgent
  type Mediator = droid.state.Mediator
  type HasContextAgent = droid.state.HasContextAgent
  type HasActivityAgent = droid.state.HasActivityAgent
  type ActivityAgent = droid.state.ActivityAgent
  type FragmentAgent = droid.state.FragmentAgent
}
