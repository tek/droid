package tryp
package droid
package api

@exportVals(Settings)
trait Vals

@export
trait Exports
extends Vals

@integrate(droid.core, view.core)
object `package`
