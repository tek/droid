package tryp
package droid

@exportNames(StringRecyclerAdapter)
trait Exports
{
  type Fab = com.melnykov.fab.FloatingActionButton
}

trait All
extends state.All
with ToIntentOps
with AndroidExt

@integrate(state)
object `package`
