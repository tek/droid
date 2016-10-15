package tryp
package droid
package state

import android.support.v7.app.ActionBarActivity

import core._

class StateActivity
extends ActionBarActivity
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
}
