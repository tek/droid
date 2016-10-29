package tryp
package droid
package integration

import android.view.Window

class IntStateActivity
extends state.StateActivity
{
  override protected def mainViewTimeout = 30 seconds

  override def onCreate(state: Bundle) {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    super.onCreate(state)
  }
}
