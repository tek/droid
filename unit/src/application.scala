package tryp
package droid
package test

class Application
extends android.app.Application
with droid.Application
with unit.UnitTestApplication
{
  def name = "tryp"
}
