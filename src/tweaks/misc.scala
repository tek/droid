package tryp.droid.tweaks

import scala.language.reflectiveCalls

import android.widget._
import android.view.Gravity
import android.content.res.ColorStateList
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.{RecyclerView,LinearLayoutManager,CardView}
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.{Toolbar ⇒ AToolbar}
import android.support.v7.app.ActionBarDrawerToggle
import android.text.{TextWatcher,TextUtils,Editable}
import android.graphics.drawable.Drawable

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._

import com.melnykov.fab.FloatingActionButton

import tryp.droid.res.{Resources,_}
import tryp.droid.TrypTextView
import tryp.droid.view.DividerItemDecoration

trait ResourcesAccess {
  protected def res(implicit c: Context,
    ns: ResourceNamespace = GlobalResourceNamespace) = Resources()

  protected def theme(implicit c: Context,
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

    def literal(value: String) =
      Tweak[TextView](_.setText(value))

    def clear = literal("")

    def content(name: String) = literal(res.s(name))

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

  def notClickable = Tweak[View](_.setClickable(false))

  def textWatcher(listener: TextWatcher) = {
    Tweak[EditText](_.addTextChangedListener(listener))
  }

  def watchText(cb: ⇒ Unit) = {
    val listener = new TextWatcher {
      def onTextChanged(cs: CharSequence, start: Int, count: Int, after: Int) {
        cb
      }

      def beforeTextChanged(cs: CharSequence, start: Int, count: Int, after:
        Int) { }

      def afterTextChanged(edit: Editable) { }
    }
    textWatcher(listener)
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

  def bgres(resid: Int) = Tweak[View](_.setBackgroundResource(resid))

  def bg(drawable: Drawable) = Tweak[View](_.setBackgroundDrawable(drawable))

  def bg(color: Int) = Tweak[View](_.setBackgroundColor(color))

  def inputType(types: Int) = Tweak[TextView](_.setInputType(types))

  def hidden = Tweak[View](_.setVisibility(android.view.View.INVISIBLE))

  def indeterminate = Tweak[ProgressBar](_.setIndeterminate(true))
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

  def linear(implicit c: Context) = {
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

  def titleColor(name: String)(implicit c: Context) = Tweak[AToolbar] {
    _.setTitleTextColor(theme.color(name))
  }

  def navButtonListener(callback: ⇒ Unit) = Tweak[AToolbar] {
    _.setNavigationOnClickListener(new android.view.View.OnClickListener {
      def onClick(v: View) = callback
    })
  }
}

object Drawer
extends ResourcesAccess
{
  type Toggle = ActionBarDrawerToggle

  private def t(f: (DrawerLayout) ⇒ Unit) = Tweak[DrawerLayout](f)

  def listener(l: DrawerLayout.DrawerListener) = t { _.setDrawerListener(l) }

  def open(edge: Int = Gravity.LEFT) = t { _.openDrawer(edge) }

  def close(edge: Int = Gravity.LEFT) = t { _.closeDrawer(edge) }

  import tryp.droid.{Screw,CanScrew}

  def sync = Screw[Toggle] { _.syncState }

  def upEnabled = Screw[Toggle] { _.setHomeAsUpIndicator(0) }
}
