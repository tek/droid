package tryp
package test

trait TrypDroidSpec
extends TestHelpers
{
  def activity: TrypActivity

  def frag[A <: Fragment: ClassTag](names: String*) =
    activity.findNestedFrag[A](names: _*)
}
