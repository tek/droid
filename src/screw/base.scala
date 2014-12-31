package tryp.droid

import android.view.MenuItem

import scala.annotation.implicitNotFound

case class Screw[-A](f: A ⇒ Unit) {
  def apply(w: A) = f(w)

  def +[A1 <: A](that: Screw[A1]): Screw[A1] = Screw { x ⇒
    this(x)
    that(x)
  }
}

@implicitNotFound("Don't know how to screw ${A} with ${B}. " + 
  "Try importing an instance of CanScrew[${A}, ${B}, ...].")
trait CanScrew[A, B, C] {
  def screw(i: A, s: B): C
}

object CanScrew {
  implicit def `screw MenuItem with Screw`[A <: MenuItem, B <: Screw[A]] =
    new CanScrew[A, B, A] {
      def screw(item: A, scrw: B) = {
        item tap { scrw(_) }
      }
    }
}

trait Screwing {
  implicit class ScrewingOps[A](w: A) {
    def <<~[S, B](t: S)(implicit canScrew: CanScrew[A, S, B]): B = {
      canScrew.screw(w, t)
    }
  }
}

object Screwing extends Screwing
