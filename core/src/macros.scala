package tryp
package droid

import reflect.macros.blackbox

trait AndroidMacros
extends MacroMetadata
{
  val c: blackbox.Context

  import c.universe._

  val actx = tq"android.content.Context"
}
