package tryp.droid.test

import tryp.droid._

trait TrypSpec
{
  def activity: TrypActivity

  def waitFor(timeout: Int)(pred: ⇒ Boolean)

  def wait(pred: ⇒ Boolean) {
    waitFor(5000)(pred)
  }

  def assertW(predicate: ⇒ Boolean) {
    assertWFor(5000)(predicate)
  }

  def assertWFor(timeout: Int)(predicate: ⇒ Boolean) {
    waitFor(timeout)(predicate)
    timeoutAssertion(predicate)
  }

  def timeoutAssertion(isTrue: Boolean)

  def frag[A <: Fragment: ClassTag](names: String*) =
    activity.findNestedFrag[A](names: _*)
}
