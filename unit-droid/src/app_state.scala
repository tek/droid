package tryp
package droid
package unit

import android.widget._

import state._

import shapeless._

trait AppStateSpecMachine
extends SimpleViewMachine

class AppStateActivityAgent(implicit a: AndroidActivityUiContext,
  res: Resources)
extends droid.state.AppStateActivityAgent
{
  implicit val activity = a.activity

  def title = "AppStateActivityAgent"

  lazy val viewMachine =
    new AppStateSpecMachine {
      lazy val layoutIO = l[FrameLayout](w[TextView] :: HNil)
    }

    override def machines = viewMachine :: super.machines

    def setView() = {
      iota.IO {
        a.activity.setContentView(safeView)
      } performMain()
    }
}
