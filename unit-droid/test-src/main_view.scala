package tryp
package droid
package unit

object MainViewSpec
extends Views[Context, AIO]
{
  import MainViewMessages._

  class Marker(c: Context)
  extends View(c)

  class MainView
  extends MainViewAgent
  {
    override def extraAdmit: Admission = {
      case MainViewReady =>
        _ << LoadUi(view2).publish
    }
  }

  lazy val view2 = ViewAgent(w[Marker])
}

class MainViewSpec
extends StateAppSpec
{
  import MainViewSpec._

  def is = s2"""
  load a different main view $loadUi
  """

  lazy val initialAgent = new MainView

  def loadUi = activity willContain view[Marker]
}
