package tryp
package droid
package core

import reflect.macros.whitebox.Context
import annotation.StaticAnnotation

class IOBaseAnnotation(val c: Context)
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
extends StaticAnnotation
{
  def macroTransform(annottees: Any*) = macro IOBaseAnnotation.process
}
