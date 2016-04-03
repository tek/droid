package tryp
package droid
package unit

import org.robolectric.Shadows

import state._
import AppState._
import MainViewMessages.LoadUi

class StartActivitySpec
extends StateAppSpec
{
  def is = s2"""
  start another activity with an agent $startActivity
  """

  override def before = stateApp.publishOne(StartActivity(MainView2()))

  def startActivity = {
    Option(Shadows.shadowOf(activity).getNextStartedActivity()) must
      beSome.eventually
  }
}
