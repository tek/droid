package tryp
package droid
package test

trait TrypDroidSpec
extends TestHelpers
{
  def activity: Activity

  def frag[A <: Fragment: ClassTag](names: String*) =
    activity.findNestedFrag[A](names)
}
