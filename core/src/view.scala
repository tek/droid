package tryp
package droid
package core

import reflect.macros.whitebox

class IOBaseAnnotation(val c: whitebox.Context)
extends Annotation
{
  import c.universe._

  object IOBaseTransformer
  extends Transformer
  {
    def apply(annottees: Annottees) = {
      annottees
    }
  }

  def transformers = IOBaseTransformer :: Nil
}

class IOBase
extends scala.annotation.StaticAnnotation
{
  def macroTransform(annottees: Any*) = macro IOBaseAnnotation.process
}
