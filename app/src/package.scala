package tryp
package droid

@exportNames(StringRecyclerAdapter)
trait Exports

trait All
extends state.All
with ToIntentOps
with AndroidExt

@integrate(state)
object `package`
