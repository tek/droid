package tryp
package droid
package unit

class Application
extends android.app.Application
with tryp.droid.Application
with UnitApplication
{
  def name = "tryp"
}
