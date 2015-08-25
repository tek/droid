package tryp.droid.test

import tryp.droid._

trait TrypSpec
extends tryp.core.TestHelpers
{
  def activity: TrypActivity

  def frag[A <: Fragment: ClassTag](names: String*) =
    activity.findNestedFrag[A](names: _*)
}
