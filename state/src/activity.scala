package tryp
package droid
package state

class StateActivity
extends Activity
with Logging
with StateStrategy
{
  def stateApp = getApplication match {
    case a: StateApplication => Right(a)
    case _ => Left("Application is not a StateApplication")
  }

  protected def mainViewTimeout = 5 seconds

  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    stateApp.fold(log.error(_), _.setActivity(this))
  }

  def agent: Option[ActivityAgent] = None
}
