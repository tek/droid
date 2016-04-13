package tryp
package droid
package unit

import view._
import state._

class AppStateSpec
extends StateAppSpec
with StreamIOViews
{
  def is = s2"""
  Application with RootAgent managing agents and activities

  load the UI of an initial Agent $run
  """

  class Marker(c: Context)
  extends View(c)

  override def initialAgent = ActivityAgent(l[FrameLayout](w[Marker]))

  def run = activity willContain view[Marker]
}
