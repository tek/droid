package tryp.droid.tweaks

import android.view.View

import macroid.Tweak
import macroid.FullDsl._

trait Slots
{
  case class Slot[A <: View](var target: Option[A] = None)
  {
    def <~(t: Tweak[A]) = target <~ t

    def foreach(f: A ⇒ Unit) {
      target foreach f
    }

    def map[B](f: A ⇒ B) = {
      target map f
    }
  }

  object Slot {
    implicit def `Option from Slot`[A <: View](s: Slot[A]) = s.target
  }

  def slut[A <: View] = new Slot[A]()

  def whore[A <: View](pimp: Slot[A]) = Tweak[A](w ⇒ pimp.target = Some(w))
}
