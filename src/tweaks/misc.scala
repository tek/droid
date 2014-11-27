package tryp.droid.tweaks

import android.app.Activity
import android.widget._
import android.view.View
import android.content.res.ColorStateList
import android.content.Context

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._

import tryp.droid.res.Resources
import tryp.droid.TrypTextView
import tryp.droid.util.Id

class Slot[A <: View](var target: Option[A] = None)
{
  def <~(t: Tweak[A]) = target <~ t
}

object Misc
{
  def image(name: String)(implicit c: Context) = {
    Tweak[ImageView](_.setImageResource(Resources().drawableId(name)))
  }

  def imageC(name: String)(implicit c: Context) = {
    Tweak[ImageView](_.setScaleType(ImageView.ScaleType.FIT_CENTER)) +
    image(name)
  }

  def shadow(color: ColorStateList, radius: Double, x: Int = 0, y: Int = 0) = {
    Tweak[TrypTextView](_.setShadow(color, radius, x, y))
  }

  def textSize(dimName: String)(implicit c: Context) = {
    Tweak[TextView](_.setTextSize(Resources().integer(dimName)))
  }

  def slut[A <: View] = new Slot[A]()

  def whore[A <: View](pimp: Slot[A]) = Tweak[A](w ⇒ pimp.target = Some(w))
}
