package tryp
package droid
package integration

class Spec
extends StateInstrumentationSpec[IntStateActivity](classOf[IntStateActivity])
{
  def intAppState = appState match {
    case a: IntAppState => a
    case _ => sys.error("no IntApp")
  }

  def mainView = intAppState.mainView

  def intView = intAppState.intView
}
