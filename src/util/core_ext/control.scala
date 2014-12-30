package tryp.droid.util

trait Control
{
  def unless(cond: Boolean)(callback: ⇒ Unit) {
    if (!cond) {
      callback
    }
  }

  implicit class `ternary operator condition`(cond: Boolean) {
    def ?[A](v: ⇒ A) = if (cond) Some(v) else None
  }

  implicit class `ternary operator alternative`[A](o: Option[A]) {
    def /[B >: A](alternative: ⇒ Option[B]) = o orElse alternative
    def /[B >: A](default: ⇒ B) = o getOrElse default
  }

  implicit class `tryp Boolean extensions`(flag: Boolean) {
    def tapIf(f: ⇒ Unit) = {
      if (flag) f
      flag
    }
  }

  implicit class Tapper[A](item: A)
  {
    def tap(fun: A ⇒ Unit): A = {
      fun(item)
      item
    }

    def tapIfEquals(cond: A)(f: A ⇒ Unit) = {
      tap { a ⇒
        if(item == cond) {
          f(item)
        }
      }
    }
  }
}

object Control extends Control
