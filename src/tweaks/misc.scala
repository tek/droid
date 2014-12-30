package tryp.droid.tweaks

import scala.language.reflectiveCalls

import android.app.Activity
import android.widget._
import android.view.View
import android.content.res.ColorStateList
import android.content.Context
import android.support.v7.widget.{RecyclerView,LinearLayoutManager,CardView}
import android.support.v7.widget.StaggeredGridLayoutManager
import android.text.{TextWatcher,TextUtils}
import android.support.v7.widget.{Toolbar ⇒ AToolbar}

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._

import com.melnykov.fab.FloatingActionButton

import tryp.droid.res.{Resources,_}
import tryp.droid.TrypTextView
import tryp.droid.view.DividerItemDecoration

trait ResourcesAccess {
  def res(implicit c: Context,
    ns: ResourceNamespace = GlobalResourceNamespace) = Resources()

  def theme(implicit c: Context,
    ns: ResourceNamespace = GlobalResourceNamespace) = {
    res.theme
  }
}

trait Text
extends ResourcesAccess
{
  case class Text(
    implicit c: Context, ns: ResourceNamespace = GlobalResourceNamespace
  )
  {
    def ellipsize(lines: Int = 1) = Tweak[TextView] { v ⇒
      if (lines > 0) v.setMaxLines(lines)
      v.setEllipsize(TextUtils.TruncateAt.END)
    }

    def shadow(color: ColorStateList, radius: Double, x: Int = 0, y: Int = 0) =
      Tweak[TrypTextView](_.setShadow(color, radius, x, y))

    def size(name: String) =
      Tweak[TextView](_.setTextSize(res.i(name)))

    def content(name: String) =
      Tweak[TextView](_.setText(res.s(name)))

    def large = macroid.contrib.TextTweaks.large

    def hint(name: String) = {
      val hint = res.s(name, Some("hint"))
      Tweak[TextView](_.setHint(hint))
    }

    def minWidth(name: String) = {
      val width = res.d(name, Some("min_width")).toInt
      Tweak[TextView](_.setMinWidth(width))
    }
  }

  def txt(
    implicit c: Context, ns: ResourceNamespace = GlobalResourceNamespace
  ) = Text()
}

trait Misc
extends Text
{
  def imageRes(name: String)(implicit c: Context) = {
    Tweak[ImageView](_.setImageResource(res.drawableId(name)))
  }

  def imageResC(name: String)(implicit c: Context) = {
    imageFitCenter + image(name)
  }

  def imageFitCenter = imageScale(ImageView.ScaleType.FIT_CENTER)

  def imageScale(sType: ImageView.ScaleType) =
    Tweak[ImageView](_.setScaleType(sType))

  def image(name: String)(implicit c: Context) = {
    Tweak[ImageView](_.setImageDrawable(theme.drawable(name)))
  }

  def imageC(name: String)(implicit c: Context) = {
    imageFitCenter + image(name)
  }

  def bgCol(name: String)(
    implicit c: Context, ns: ResourceNamespace = GlobalResourceNamespace
  ) = {
    val col = theme.color(ns.format(name, Some("bg"))).toInt
    Tweak[View](_.setBackgroundColor(col))
  }

  def checked(state: Boolean) = {
    Tweak[CheckBox](_.setChecked(state))
  }

  def clickable = Tweak[View](_.setClickable(true))

  def textWatcher(listener: TextWatcher) = {
    Tweak[EditText](_.addTextChangedListener(listener))
  }

  type canSetColor = View { def setColor(i: Int) }

  def color(name: String)(implicit c: Context) = Tweak[canSetColor] {
    _.setColor(theme.color(name))
  }

  object Fab
  extends ResourcesAccess
  {
    def colors(normal: String, pressed: String)(implicit c: Context) =
      Tweak[FloatingActionButton] { fab ⇒
        fab.setColorNormal(theme.color(normal))
        fab.setColorPressed(theme.color(pressed))
      }
  }
}

object Misc extends Misc

object Spinner
extends ResourcesAccess
{
  def adapter(a: SpinnerAdapter) = {
    Tweak[Spinner](_.setAdapter(a))
  }
}

object Recycler
extends ResourcesAccess
{
  def recyclerAdapter(a: RecyclerView.Adapter[_]) = {
    Tweak[RecyclerView](_.setAdapter(a))
  }

  def linearLayoutManager(implicit c: Context) = {
    Tweak[RecyclerView](_.setLayoutManager(new LinearLayoutManager(c)))
  }

  def stagger(
    count: Int, orientation: Int = StaggeredGridLayoutManager.VERTICAL
  )(implicit c: Context) = {
    Tweak[RecyclerView](_.setLayoutManager(
      new StaggeredGridLayoutManager(count, orientation)))
  }

  def divider(implicit c: Context) = Tweak[RecyclerView](
    _.addItemDecoration(new DividerItemDecoration(c, null))
  )
}

object Toolbar
extends ResourcesAccess
{
  def minHeight(height: Int)(implicit c: Context) = {
    Tweak[AToolbar](_.setMinimumHeight(height))
  }

  def title(value: String) = {
    Tweak[AToolbar](_.setTitle(value))
  }

  def logo(resid: Int) = Tweak[AToolbar](_.setLogo(resid))
}
