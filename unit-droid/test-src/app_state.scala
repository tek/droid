package tryp
package droid
package unit

import state.core._
import state._
import view.core._
import view._

import android.support.v7.widget.RecyclerView
import android.widget._

import org.robolectric.annotation.Config

@Config(application = classOf[StateApplication])
class AppStateSpec
extends ActivitySpec[StateAppActivity]
{
  def is = s2"""
  run $run
  """

  def activityClass = classOf[StateAppActivity]

  def run = activity willContain view[EditText]
}
