package tryp.droid.util

trait Control
{
  def unless(cond: Boolean)(callback: ⇒ Unit) {
    if (!cond) {
      callback
    }
  }
}
