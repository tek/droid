package tryp.droid.tweaks

import scala.collection.mutable.ListBuffer

import android.widget._
import android.support.v7.widget._
import RelativeLayout._
import android.view.{View,ViewGroup,Gravity}
import ViewGroup.LayoutParams._
import android.app.Activity
import android.graphics.drawable.Drawable
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.widget.{Toolbar ⇒ AToolbar}

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._
import RuleRelativeLayout.Rule

import tryp.droid.view.ActivityContexts
import tryp.droid.res.Resources
import tryp.droid.Macroid._

trait Layout
{
  def above(d: Int) = Rule(ABOVE, d)
  def below(d: Int) = Rule(BELOW, d)
  def rightOf(d: Int) = Rule(RIGHT_OF, d)
  def leftOf(d: Int) = Rule(LEFT_OF, d)

  val ↤ = Rule(ALIGN_PARENT_LEFT)
  val ↦ = Rule(ALIGN_PARENT_RIGHT)
  val ↥ = Rule(ALIGN_PARENT_TOP)
  val ↧ = Rule(ALIGN_PARENT_BOTTOM)

  def alignBottom(d: Int) = Rule(ALIGN_BOTTOM, d)

  val centerv = Rule(CENTER_VERTICAL)
  val centerh = Rule(CENTER_HORIZONTAL)
  val ⇹ = centerh

  val ↔ = Width(MATCH_PARENT)
  val ↕ = Height(MATCH_PARENT)

  case class Width(value: Int)
  case class Height(value: Int)

  implicit def int2width(value: Int): Width = Width(value)
  implicit def int2height(value: Int): Height = Height(value)
  implicit def float2width(value: Float): Width = Width(value.toInt)
  implicit def float2height(value: Float): Height = Height(value.toInt)

  implicit def `Widget is tweakable with Height`[W <: View] =
    new CanTweak[W, Height, W] {
      def tweak(w: W, h: Height) = Ui { llp(height = h)(w); w }
    }

  implicit def `Widget is tweakable with Width`[W <: View] =
    new CanTweak[W, Width, W] {
      def tweak(w: W, h: Width) = Ui { llp(width = h)(w); w }
    }

  implicit def `Tweak from Height`[W <: View] (h: Height) = llp(height = h)

  implicit def `Tweak from Width`[W <: View](w: Width) = llp(width = w)

  def extractRlParams(params: Any*) = {
    val rules = ListBuffer[Rule]()
    var width = Width(WRAP_CONTENT)
    var height = Height(WRAP_CONTENT)
    params foreach { param ⇒
      param match {
        case r: Rule ⇒ rules += r
        case w: Width ⇒ width = w
        case h: Height ⇒ height = h
        case p ⇒ throw new Exception(
          s"Invalid parameter for relative layout: ${p}[${p.getClass}]")
      }
    }
    (width, height, rules)
  }

  def relative(params: Any*) = {
    val (width, height, rules) = extractRlParams(params: _*)
    lp[RuleRelativeLayout](width.value, height.value, rules: _*)
  }

  def rlp(params: Any*) = relative(params: _*)

  def llp(width: Width = WRAP_CONTENT, height: Height = WRAP_CONTENT, weight:
    Int = 0): Tweak[View] = {
    lp[LinearLayout](width.value, height.value, weight)
  }

  def dlp(width: Width = WRAP_CONTENT, height: Height = WRAP_CONTENT, gravity:
    Int = Gravity.START): Tweak[View] = {
    lp[DrawerLayout](width.value, height.value, gravity)
  }

  def flp(width: Width = WRAP_CONTENT, height: Height = WRAP_CONTENT, gravity:
    Int = Gravity.START): Tweak[View] = {
    lp[FrameLayout](width.value, height.value, gravity)
  }

  def vlp(width: Width = WRAP_CONTENT, height: Height = WRAP_CONTENT) = {
    lp[ViewGroup](width.value, height.value)
  }

  def tweakSum[A <: View](tweaks: Tweak[A]*): Tweak[A] = {
    tweaks.foldLeft(Tweak[A](a ⇒ Unit))((a, b) ⇒ a + b)
  }

  def foreground(res: Drawable) = Tweak[FrameLayout](_.setForeground(res))

  def selectableFg(implicit c: Context) =
    foreground(Resources().theme.drawable("selectableItemBackground"))

  def selectable(implicit c: Context) =
    bg(Resources().theme.drawable("selectableItemBackground"))

  def elevation(dist: Float) = Tweak[CardView](_.setCardElevation(dist))

  def cornerRadius(dist: Float) = Tweak[CardView](_.setRadius(dist))

  def contentPadding(
    left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0, all: Int = -1
  ) = Tweak[CardView] { v ⇒
    if (all == -1)
      v.setContentPadding(left, top, right, bottom)
    else
      v.setContentPadding(all, all, all, all)
  }

  def cardBackgroundColor(color: Int) = 
    Tweak[CardView](_.setCardBackgroundColor(color))

  def fitsSystemWindows = Tweak[View](_.setFitsSystemWindows(true))

  object FL
  extends ActivityContexts
  {
    def apply(tweaks: Tweak[FrameLayout]*)(children: Ui[View]*)(
      implicit a: Activity) = {
      l[FrameLayout](children: _*) <~ tweakSum(tweaks: _*)
    }
  }

  def clickFrame(dispatch: ⇒ Unit)(ui: Ui[View]*)(implicit a: Activity) = {
    FL(selectableFg)(ui: _*) <~ On.click {
      dispatch
      Ui.nop
    }
  }

  object CV
  extends ActivityContexts
  {
    def apply(tweaks: Tweak[CardView]*)(children: Ui[View]*)(
      implicit a: Activity) = {
      l[CardView](children: _*) <~ tweakSum(tweaks: _*)
    }
  }

  object LL
  extends ActivityContexts
  {
    def apply(tweaks: Tweak[LinearLayout]*)(children: Ui[View]*)(
      implicit a: Activity) = {
      l[LinearLayout](children: _*) <~ tweakSum(tweaks: _*)
    }
  }

  object RL
  extends ActivityContexts
  {
    def apply(tweaks: Tweak[RelativeLayout]*)(children: Ui[View]*)(
      implicit a: Activity) = {
      l[RelativeLayout](children: _*) <~ tweakSum(tweaks: _*)
    }
  }
}

object Layout extends Layout
