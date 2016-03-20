package tryp
package droid
package unit

class Application
extends android.app.Application
with droid.Application
with UnitApplication
{
  def name = "tryp"
}

class StateApplication
extends Application
with state.StateApplication
{
  def initialAgent(a: Activity) = {
    implicit val uiCtx = AndroidActivityUiContext.default(a)
    implicit val res = droid.core.Resources.fromContext(a)
    new AppStateActivityAgent
  }
}

class MainViewStateApplication
extends Application
with state.StateApplication
{
  def initialAgent(a: Activity) = new MainViewAA
}
