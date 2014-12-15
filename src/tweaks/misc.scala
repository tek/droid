package tryp.droid.tweaks

import android.app.Activity
import android.widget._
import android.view.View
import android.content.res.ColorStateList
import android.content.Context
import android.support.v7.widget.{RecyclerView,LinearLayoutManager,CardView}
import android.text.TextWatcher

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._

import tryp.droid.res.{Resources,_}
import tryp.droid.TrypTextView
import tryp.droid.util.Id

trait Misc
{
  class Slot[A <: View](var target: Option[A] = None)
  {
    def <~(t: Tweak[A]) = target <~ t
  }

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

  def hint(name: String)(
    implicit c: Context, ns: ResourceNamespace = GlobalResourceNamespace
  ) = {
    val hint = Resources().string(ns.format(s"${name}_hint"))
    Tweak[TextView](_.setHint(hint))
  }

  def minWidthDim(dimName: String)(
    implicit c: Context, ns: ResourceNamespace = GlobalResourceNamespace
  ) = {
    val minW = Resources().dimen(ns.format(s"${dimName}_min_width")).toInt
    Tweak[TextView](_.setMinWidth(minW))
  }

  def slut[A <: View] = new Slot[A]()

  def whore[A <: View](pimp: Slot[A]) = Tweak[A](w ⇒ pimp.target = Some(w))

  def checked(state: Boolean) = {
    Tweak[CheckBox](_.setChecked(state))
  }

  def clickable = Tweak[View](_.setClickable(true))

  def textWatcher(listener: TextWatcher) = {
    Tweak[EditText](_.addTextChangedListener(listener))
  }
}

object Misc extends Misc

object Spinner
{
  def adapter(a: SpinnerAdapter) = {
    Tweak[Spinner](_.setAdapter(a))
  }
}

object Recycler
{
  def recyclerAdapter(a: RecyclerView.Adapter[_]) = {
    Tweak[RecyclerView](_.setAdapter(a))
  }

  def linearLayoutManager(implicit c: Context) = {
    Tweak[RecyclerView](_.setLayoutManager(new LinearLayoutManager(c)))
  }
}
