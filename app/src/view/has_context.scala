package tryp
package droid

import simulacrum._

@typeclass trait HasContextF[A]
{
  implicit def context(a: A): Context

  implicit def res(a: A): Resources = core.Resources.fromContext(context(a))

  // implicit lazy val settings = implicitly[Settings]
}

object HasContextF
{
  implicit def contextHasContext[A <: Context] =
    new HasContextF[A] {
      def context(c: A) = c
    }

  // implicit def activityHasContext[A <: Activity] =
  //   new HasContextF[A] {
  //     def context(a: A) = a
  //   }

  implicit def fragmentHasContext[A <: Fragment] =
    new HasContextF[A] {
      def context(f: A) = f.getActivity
    }
}

class ContextOps[A: HasContextF](a: A)
{
  def res = a.res
}
