package tryp
package droid
package integration

abstract class Spec
extends SpecsSpec[IntStateActivity](classOf[IntStateActivity])
{
  def intAppState = appState match {
    case a: IntAppState => a
    case _ => sys.error("no IntApp")
  }

  def mainView = intAppState.mainView

  def intView = intAppState.intView
}
