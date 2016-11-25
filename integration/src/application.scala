package tryp
package droid
package integration

import droid.state.StateApplicationAgent
import droid.state.AppState.SetActivity

class IntApplication
extends android.app.Application
with StateApplication
with BoundedCachedPool { ia =>
  def name = "app"

  lazy val root = new StateApplicationAgent {
    def name = "state_app"
    def agents = Nil
    override def initialAgent =
      Some(new Simple)
    def transitions(comm: MComm) = tryp.state.NoTrans
    def strat = ia.strategy
  }
  
  lazy val agents = null
    // tryp.state.Agents(Stream(root)).run.runLog.unsafeRun()

  override def onCreate(): Unit = {
    agents
    super.onCreate()
  }

  // def setActivity(act: Activity) = {
  //   root.schedule1(SetActivity(act).toLocal)
  // }
}
