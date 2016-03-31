package tryp
package droid
package unit

class Application
extends android.app.Application
{
  def name = "tryp"
}

class StateApplication
extends Application
with state.StateApplication
{
  def initialAgent = new Agent1
}

class MainViewStateApplication
extends Application
with state.StateApplication
{
  def initialAgent = new MainView1
}
