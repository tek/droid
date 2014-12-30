package tryp.droid.util

trait OptionExt
{
  import Control._

  implicit class `tryp Option extensions`[A](o: Option[A]) {
    def ifNone(f: ⇒ Unit) = {
      o.isEmpty ? f
      o
    }
  }
}
