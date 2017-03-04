package tryp
package droid

@exportNames(StringRecyclerAdapter)
trait Exports
{
  type Fab = com.melnykov.fab.FloatingActionButton
}

trait All
extends view.All
with ToIntentOps
with AndroidExt

@integrate(view)
object `package`
