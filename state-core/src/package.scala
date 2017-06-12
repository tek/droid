package tryp
package droid
package state
package core

@exportTypes(AnnotatedTAIO)
trait Types

@exportNames(ContextAIO, ActivityAIO, AppCompatActivityAIO)
trait Names

@export
trait Exports
extends Types
with Names

trait All
extends ToAIOStateOps
with AIOParcel

@integrate(view, tryp.state.core, tryp.state)
object `package`
