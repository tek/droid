package tryp
package droid
package state
package core

@exportTypes(AnnotatedTIO)
trait Types

@exportNames(ContextIO, ActivityIO, AppCompatActivityIO)
trait Names

@export
trait Exports
extends Types
with Names

trait All
extends ToIOStateOps
with IOParcel

@integrate(view, tryp.state.core, tryp.state)
object `package`
