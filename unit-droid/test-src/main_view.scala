package tryp
package droid
package unit

import android.support.v7.widget.RecyclerView
import android.widget._

import org.robolectric.annotation.Config
import org.robolectric.Shadows

import state._
import AppState._
import MainViewMessages.LoadUi

@Config(application = classOf[MainViewStateApplication])
class MainViewSpec
extends StateAppSpec
with ResourcesAccess
{
  def is = s2"""
  run $run
  """

  def before = {
  }

  sequential

  def run = {
    activity
    Thread.sleep(5000)
    stateApp.publishOne(LoadUi(new Agent3))
    activity willContain view[EditText]
    // val v = new MainViewAA2
    // stateApp.publishOne(StartActivity(v))
    // Some(Shadows.shadowOf(activity).getNextStartedActivity()) must
    //   beSome.eventually
  }
}
