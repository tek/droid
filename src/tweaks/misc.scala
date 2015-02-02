package tryp.droid.tweaks

import scala.language.reflectiveCalls

import android.widget._
import android.support.v7.widget._
import android.view.{Gravity,MotionEvent}
import android.content.res.ColorStateList
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.{RecyclerView,LinearLayoutManager,CardView}
import android.support.v7.widget.StaggeredGridLayoutManager
import android.support.v7.widget.{Toolbar ⇒ AToolbar}
import android.support.v7.app.ActionBarDrawerToggle
import android.text.{TextWatcher,TextUtils,Editable}
import android.graphics.drawable.Drawable

import android.transitions.everywhere.TransitionManager

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._

import akka.actor.ActorSelection

import com.melnykov.fab.FloatingActionButton

import tryp.droid.res.{Resources,_}
import tryp.droid.view.{TrypTextView,DividerItemDecoration,ParallaxHeader}
import tryp.droid.Messages

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

    def hintLiteral(hint: String) = {
      Tweak[TextView](_.setHint(hint))
    }

    def hint(name: String) = {
      val hint = res.s(name, Some("hint"))
      hintLiteral(hint)
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
    val col = res.c(name, Some("bg")).toInt
    Tweak[View](_.setBackgroundColor(col))
  }

  def cardBackgroundColor(color: String)(implicit c: Context) = 
    Tweak[CardView](_.setCardBackgroundColor(res.c(color, Some("bg"))))

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
    _.setColor(res.c(name))
  }

  object Fab
  extends ResourcesAccess
  {
    def colors(normal: String, pressed: String)(implicit c: Context) =
      Tweak[FloatingActionButton] { fab ⇒
        fab.setColorNormal(res.c(normal))
        fab.setColorPressed(res.c(pressed))
      }
  }

  def bgres(resid: Int) = Tweak[View](_.setBackgroundResource(resid))

  def bg(drawable: Drawable) = Tweak[View](_.setBackgroundDrawable(drawable))

  def bg(color: Int) = Tweak[View](_.setBackgroundColor(color))

  def inputType(types: Int) = Tweak[TextView](_.setInputType(types))

  def hidden = Tweak[View](_.setVisibility(android.view.View.INVISIBLE))

  def indeterminate = Tweak[ProgressBar](_.setIndeterminate(true))

  def transitionName(name: String) = Tweak[View] { v ⇒
    TransitionManager.setTransitionName(v, name)
  }

  def translateY(y: Int) = Tweak[View] { _.setTranslationY(y) }

  def setTop(y: Int) = Tweak[View] { _.setTop(y) }

  def invalidate = Tweak[View] { _.invalidate() }

  def relayout = Tweak[View] { _.requestLayout() }

  def parallaxScroll(y: Int) = Tweak[ParallaxHeader] { _.set(y) }
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
  val t = Tweak[RecyclerView] _

  def recyclerAdapter(a: RecyclerView.Adapter[_]) = t(_.setAdapter(a))

  def linear(implicit c: Context) = {
    t(_.setLayoutManager(new LinearLayoutManager(c)))
  }

  def layoutManager(m: RecyclerView.LayoutManager)(implicit c: Context) =
    t(_.setLayoutManager(m))

  def stagger(
    count: Long, orientation: Int = StaggeredGridLayoutManager.VERTICAL
  )(implicit c: Context) =
    layoutManager(new StaggeredGridLayoutManager(count.toInt, orientation))

  def grid(count: Long)(implicit c: Context) =
    layoutManager(new GridLayoutManager(c, count.toInt))

  def divider(implicit c: Context) = t(
    _.addItemDecoration(new DividerItemDecoration(c, null))
  )

  def dataChanged(implicit c: Context) = t {
    _.getAdapter.notifyDataSetChanged
  }

  def onScroll(callback: (ViewGroup, Int) ⇒ Unit)(implicit c: Context) = {
    t {
      val listener = new RecyclerView.OnScrollListener {
        var height = 0

        override def onScrolled(view: RecyclerView, dx: Int, dy: Int) {
          super.onScrolled(view, dx, dy)
          height += dy
          callback(view, height)
        }
      }
      _.setOnScrollListener(listener)
    }
  }

  def onScrollActor(actor: ActorSelection)(implicit c: Context) =
    onScroll((view, y) ⇒ actor ! Messages.Scrolled(view, y))

  def reverseLayout(implicit c: Context) = t {
    _.getLayoutManager match {
      case m: LinearLayoutManager ⇒ m.setReverseLayout(true)
      case m ⇒ Log.e(s"Used reverseLayout on incompatible type ${m.className}")
    }
  }

  def scrollTop(implicit c: Context) = t { rv ⇒
    rv.scrollToPosition(rv.getAdapter.getItemCount - 1)
  }
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
    _.setTitleTextColor(res.c(name))
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

case class InvTransformer(f: PartialFunction[View, Ui[Any]]) {
  def apply(w: View): Unit = {
    f.lift.apply(w).foreach(_.get)
    w.getParent match {
      case v: View ⇒ apply(v)
      case _ ⇒
    }
  }
}
