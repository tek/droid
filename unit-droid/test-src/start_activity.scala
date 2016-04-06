package tryp
package droid
package unit

import org.robolectric.Shadows

import state._
import AppState._

class StartActivitySpec
extends StateAppSpec
{
  def is = s2"""
  start another activity with an agent $startActivity
  """

  def initialAgent = new MainViewAgent {}

  override def before = {
    super.before
    stateApp.publishOne(StartActivity(new MainViewAgent {}))
  }

  def startActivity = {
    Option(Shadows.shadowOf(activity).getNextStartedActivity()) must
      beSome.eventually
  }
}
