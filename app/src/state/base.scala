package tryp
package droid
package state

import shapeless.HNil
import shapeless.tag.@@

trait DroidMachineBase[A <: AndroidUiContext]
extends Machine
{
  implicit def ctx: A
}

abstract class DroidMachine[A <: AndroidUiContext]
(implicit val ctx: A, mt: MessageTopic @@ To)
extends DroidMachineBase[A]

trait SimpleDroidMachine
extends DroidMachine[AndroidUiContext]

trait ActivityDroidMachine
extends DroidMachine[AndroidActivityUiContext]

abstract class DroidDBMachine
(implicit db: tryp.slick.DbInfo, ctx: AndroidActivityUiContext,
  mt: MessageTopic @@ To)
extends ActivityDroidMachine
