package tryp
package droid
package trial

import state._

class TApplication
extends android.app.Application
with Application
with StateApplication
with MultiDexApplication
{
  override val useDb = false

  def name = "tryp"

  def initialAgent = new TMainViewAgent
}
