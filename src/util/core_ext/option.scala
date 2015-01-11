package tryp.droid.util

trait OptionExt
{
  implicit class `Option extensions`[A](o: Option[A]) {
    def tap(f: A ⇒ Unit): Option[A] = {
      o foreach(f)
      o
    }
  }
}
