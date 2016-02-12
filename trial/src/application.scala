package tryp
package droid
package trial

class TApplication
extends android.app.Application
// with state.StateApplication
with droid.Application
{
  override val useDb = false

  // def context = getApplicationContext

  def name = "tryp"
}
