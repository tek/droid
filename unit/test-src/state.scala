package tryp
package droid
package unit

import org.specs2._

class DummyAndroidUiContext
extends AndroidUiContext
{
  def loadFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def transitionFragment(fragment: FragmentBuilder) = Ui("not implemented")

  def showViewTree(view: View) = "not implemented"

  def notify(id: String): Ui[Any] = Ui("asdf")
}

class MachineSpec
extends Specification
{
  def is = s2"""
  machine $machine
  """

  def machine = {
    implicit val c = new DummyAndroidUiContext
    val sf = new Agent {
    }
    sf.runState()
    sf.send(Resume)
    Thread.sleep(2000)
    sf.send(Resume)
    Thread.sleep(2000)
    1 === 1
  }
}
