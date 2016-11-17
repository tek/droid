package tryp
package droid
package integration

import droid.state.StateApplicationAgent
import droid.state.AppState.SetActivity

class IntApplication
extends android.app.Application
with StateApplication
{
  lazy val root = new StateApplicationAgent {

    override def initialAgent =
      Some(new IntMainViewAgent)
  }

  override def onCreate(): Unit = {
    root.runAgent()
    super.onCreate()
  }

  def setActivity(act: Activity) = {
    root.schedule1(SetActivity(act).toLocal)
  }
}
