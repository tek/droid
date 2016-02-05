package tryp
package droid
package unit

import android.support.v7.widget.RecyclerView
import android.widget._

import org.robolectric.annotation.Config

@Config(application = classOf[StateApplication])
class AppStateSpec
extends ActivitySpec[StateAppViewActivity]
{
  def is = s2"""
  run $run
  """

  def before = {
  }

  def activityClass = classOf[StateAppViewActivity]

  sequential

  def run = {
    Thread.sleep(1000)
    activity.showViewTree()
    1 === 1
  }
}
