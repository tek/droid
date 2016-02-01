package tryp
package droid

import simulacrum._

@typeclass trait BasicF[A]
extends HasContextF[A]
with Logging
{
  type RIdTypes = Int with String with RId

  def systemService[B: ClassTag](a: A)(name: String) = {
    context(a).getSystemService(name) match {
      case a: B ⇒ a
      case _ ⇒ {
        throw new ClassCastException(
          s"Wrong class for ${implicitly[ClassTag[B]].className}!"
        )
      }
    }
  }
}
