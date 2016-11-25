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

@integrate(state, tryp.state.StateDecls)
object `package`
