package tryp.droid.tweaks

import scala.concurrent.{ExecutionContext,Future}

import macroid.{Tweak,CanTweak,Snail}
import macroid.FullDsl._

trait Slots
{
  case class Slot[A <: View](var target: Option[A] = None)
  {
    def <~(t: Tweak[A]) = target <~ t

    def <~(t: Option[Tweak[A]]) = target <~ t

    def <~~(s: Snail[A])(implicit ec: ExecutionContext) = target <~~ s

    def foreach(f: A ⇒ Unit) {
      target foreach f
    }

    def map[B](f: A ⇒ B) = {
      target map f
    }
  }

  object Slot {
    implicit def `Option from Slot`[A <: View](s: Slot[A]) = s.target

    implicit def `Widget is tweakable with Slot`[W <: View] =
      new CanTweak[W, Slot[W], W] {
        def tweak(w: W, s: Slot[W]) = w <~ whore(s)
      }
  }

  def slut[A <: View] = new Slot[A]()

  def whore[A <: View](pimp: Slot[A]) = Tweak[A](w ⇒ pimp.target = Some(w))
}
