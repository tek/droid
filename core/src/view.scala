package tryp
package droid
package core

import reflect.macros.whitebox

class AIOBaseAnnotation(val c: whitebox.Context)
extends Annotation
{
  import c.universe._

  object AIOBaseTransformer
  extends Transformer
  {
    def apply(annottees: Annottees) = {
      annottees
    }
  }

  def transformers = AIOBaseTransformer :: Nil
}

class AIOBase
extends scala.annotation.StaticAnnotation
{
  def macroTransform(annottees: Any*) = macro AIOBaseAnnotation.process
}
