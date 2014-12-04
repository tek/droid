package tryp.droid

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.{ Context ⇒ MacroContext }

object MacrosTest
{
  def instFrag[F]: F = macro instFragImpl[F]

  def instFragImpl[F: c.WeakTypeTag](c: MacroContext) = {
    import c.universe._
    q"new ${weakTypeOf[F]}"
  }
}
