package tryp
package droid
package unit

import android.support.v7.widget.RecyclerView
import android.widget._

import org.robolectric.annotation.Config
import org.robolectric.Shadows

import state._
import AppState._

@Config(application = classOf[MainViewStateApplication])
class MainViewSpec
extends StateAppSpec[UnitActivity1]
{
  def is = s2"""
  run $run
  """

  def before = {
  }

  def activityClass = classOf[UnitActivity1]

  sequential

  def run = {
    val v = new MainView2
    stateApp.publishOne(StartActivity(v))
    Some(Shadows.shadowOf(activity).getNextStartedActivity()) must
      beSome.eventually
  }
}
