package tryp
package droid
package integration

class IntApplication
extends android.app.Application
with StateApplication
with MultiDexApplication
{
  override def initialAgent = Some(new IntMainViewAgent2)
}
