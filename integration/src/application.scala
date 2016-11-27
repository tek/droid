package tryp
package droid
package integration

import java.util.concurrent._

import droid.state.AppState.SetActivity

class IntApplication
extends android.app.Application
with StateApplication
{ ia =>
  def name = "app"

  lazy val root =
    new StateApplicationAgent {
      def strat = ia.strategy
    }

  override def onCreate(): Unit = {
    agents
    super.onCreate()
  }

  def setActivity(act: Activity) = {
    agents.unsafeSend(SetActivity(act))
  }
}
