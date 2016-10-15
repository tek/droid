package tryp
package droid
package trial

class TApplication
extends android.app.Application
with StateApplication
with MultiDexApplication
{
  override def initialAgent = Some(new TMainViewAgent)
}
