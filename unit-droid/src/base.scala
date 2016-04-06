package tryp
package droid
package unit

import state._
import view._

class SpecActivity
extends Activity
{
  // lazy val navigation = {
  //   Navigation.simple(NavigationTarget("test", frag))
  // }

  def frag: () => Fragment = ???
}

abstract class TestViewActivity
extends Activity
with ViewActivity

abstract class ActivitySpec[A <: Activity]
extends UnitSpecs2Spec[A]
with Matchers
{
  def before = ()
}
