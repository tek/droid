package tryp
package droid
package unit

import view.core._
import view._
import state._

object MainViewSpec
extends Views[Context, StreamIO]
{
  import AppState._
  import MainViewMessages._

  class Marker(c: Context)
  extends View(c)

  class MainView
  extends MainViewAgent
  {
    override def extraAdmit: Admission = {
      case ContentViewReady(ag) if ag == this =>
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
