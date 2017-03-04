package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity

import AppState._

class StateActivity
extends AppCompatActivity
with Logging
{
  lazy val stateApp = getApplication match {
    case a: StateApplication => a
    case _ => sys.error("application is not a StateApplication")
  }

  lazy val agents = stateApp.agents

  import stateApp.{strategy, scheduler}

  protected def mainViewTimeout = 5 seconds

  override def onCreate(saved: Bundle) = {
    super.onCreate(saved)
    agents.send(SetActivity(this)) !? s"send SetActivity($this)"
  }

  override def onStart() = {
    super.onStart()
    agents.send(OnStart(this)) !? s"send OnStart($this)"
  }

  override def onResume() = {
    super.onResume()
    agents.send(OnResume(this)) !? s"send OnResume($this)"
  }

  def agent: Option[ActivityAgent] = None
}
