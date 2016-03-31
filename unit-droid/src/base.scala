package tryp
package droid
package unit

import android.widget._
import android.support.v7.widget.RecyclerView

import scalaz._, Scalaz._

import droid.state._
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
