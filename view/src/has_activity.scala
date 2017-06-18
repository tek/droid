package tryp
package droid
package view

@tc trait HasActivity[A]
{
  implicit def activity(a: A): Activity
}

object HasActivity
{
  implicit def activityHasActivity[A <: Activity] =
    new HasActivity[A] {
      def activity(a: A) = a
    }

  implicit def fragmentHasActivity[A <: Fragment] =
    new HasActivity[A] {
      def activity(a: A) = a.getActivity
    }
}
