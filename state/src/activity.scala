package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity

class StateActivity
extends AppCompatActivity
with Logging
{
  def stateApp = getApplication match {
    case a: StateApplication => Right(a)
    case _ => Left("Application is not a StateApplication")
  }

  // def stateAppAgent = stateApp map (_.stateAppAgent)
  
  def agents = stateApp map (_.agents)

  // def state[A](f: StateApplicationAgent => A) =
  //   stateAppAgent.fold(log.error(_), f)

  protected def mainViewTimeout = 5 seconds

  override def onCreate(saved: Bundle) = {
    super.onCreate(saved)
    // agents.map(_.send(AppState.SetActivity(this))).unsafeRun()
    // state(_.setActivity(this))
  }

  override def onStart() = {
    super.onStart()
    // state(_.onStart(this))
  }

  override def onResume() = {
    super.onResume()
    // state(_.onResume(this))
  }

  def agent: Option[ActivityAgent] = None
}
