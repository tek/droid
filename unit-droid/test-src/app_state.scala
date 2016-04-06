package tryp
package droid
package unit

import state._

object AppStateSpec
{
  class Marker(c: Context)
  extends View(c)

  class SpecAgent
  extends ActivityAgent
  {
    lazy val viewMachine =
      new ViewMachine {
        lazy val layoutIO = l[FrameLayout](w[Marker])
      }
  }
}

class AppStateSpec
extends StateAppSpec
{
  import AppStateSpec._

  def is = s2"""
  Application with RootAgent managing agents and activities

  load the UI of an initial Agent $run
  """

  override def initialAgent = new SpecAgent

  def run = activity willContain view[Marker]
}
