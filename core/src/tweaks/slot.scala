package tryp.droid.tweaks

import scala.concurrent.ExecutionContext

import macroid._

import tryp.droid.WidgetBase

case class Slot[A <: View](var target: Option[A] = None)
{
  def <~(t: Tweak[A]) = target <~ t

  def <~(w: WidgetBase[A]) = target <~ w.tweak

  def <~(t: Option[Tweak[A]]) = target <~ t

  def <~~(s: Snail[A])(implicit ec: ExecutionContext) = target <~~ s

  def foreach(f: A ⇒ Unit) {
    target foreach f
  }

  def map[B](f: A ⇒ B) = {
    target map f
  }

  def some[B](f: A ⇒ B) = {
    target some(f)
  }
}

object Slot
extends Slots
{
  implicit def `Option from Slot`[A <: View](s: Slot[A]) = s.target

  implicit def `Widget is tweakable with Slot`[W <: View] =
    new CanTweak[W, Slot[W], W] {
      def tweak(w: W, s: Slot[W]) = w <~ whore(s)
    }
}

trait Slots
{
  def slut[A <: View] = new Slot[A]()

  def whore[A <: View](pimp: Slot[A]) = Tweak[A](w ⇒ pimp.target = Some(w))
}

object Slots
extends Slots
