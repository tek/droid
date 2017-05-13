package tryp
package droid
package recycler

@export
trait Exports

@integrate(droid.state.core, droid.state, droid.view, droid.core, tryp.state, tryp.state.core)
object `package`
{
  type AnyTree = state.AnyTree
}
