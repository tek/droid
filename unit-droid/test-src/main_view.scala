package tryp
package droid
package unit

import android.support.v7.widget.RecyclerView
import android.widget._

import org.robolectric.annotation.Config

@Config(application = classOf[MainViewStateApplication])
class MainViewSpec
extends ActivitySpec[UnitActivity1]
{
  def is = s2"""
  run $run
  """

  def before = {
  }

  def activityClass = classOf[UnitActivity1]

  sequential

  def run = {
    activity willContain view[EditText]
  }
}
