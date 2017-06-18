package tryp
package droid
package view

@tc trait HasContext[A]
{
  implicit def context(a: A): Context

  implicit def res(a: A): Resources =
    droid.core.Resources.fromContext(context(a))
}

object HasContext
{
  implicit def contextHasContext[A <: Context] =
    new HasContext[A] {
      def context(c: A) = c
    }

  implicit def fragmentHasContext[A <: Fragment] =
    new HasContext[A] {
      def context(f: A) = f.getActivity
    }
}
