package tryp.droid

import android.view.MenuItem

import scala.annotation.implicitNotFound
import scala.language.higherKinds

import macroid.util.Effector

case class Screw[-A](f: A => Unit) {
  def apply(w: A) = {
    f(w)
  }

  def +[A1 <: A](that: Screw[A1]): Screw[A1] = Screw { x =>
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
  implicit def `screw Any with Screw`[A, B <: Screw[A]] =
    new CanScrew[A, B, A] {
      def screw(item: A, scrw: B) = {
        item tap(scrw.apply)
      }
    }

  implicit def `Effector is screwable`[W, F[+_], T, R]
  (implicit effector: Effector[F], canScrew: CanScrew[W, T, R]) =
    new CanScrew[F[W], T, F[W]] {
      def screw(f: F[W], t: T) = {
        effector.foreach(f) { w =>
          canScrew.screw(w, t)
          Ui.nop
        }
        f
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
