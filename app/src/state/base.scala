package tryp
package droid
package state

import shapeless.HNil
import shapeless.tag.@@

trait DroidMachine[A <: AndroidUiContext]
extends Machine
{
  implicit def ctx: A
}

abstract class SimpleDroidMachine
(implicit val ctx: AndroidUiContext)
extends DroidMachine[AndroidUiContext]

trait ActivityDroidMachine
extends DroidMachine[AndroidHasActivityUiContext]

trait DroidDBMachine
extends ActivityDroidMachine
{
  implicit def db: tryp.slick.DbInfo
}
