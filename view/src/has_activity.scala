package tryp
package droid
package view

import simulacrum._

@typeclass abstract class HasActivityF[A]
extends HasContextF[A]
{
  implicit def activity(a: A): Activity

  override def context(a: A): Context = activity(a)
}

object HasActivityF
{
  implicit def activityHasActivity[A <: Activity] =
    new HasActivityF[A] {
      def activity(a: A) = a
    }

  implicit def fragmentHasActivity[A <: Fragment] =
    new HasActivityF[A] {
      def activity(a: A) = a.getActivity
    }
}
