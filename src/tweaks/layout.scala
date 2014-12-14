package tryp.droid.tweaks

import scala.collection.mutable.ListBuffer

import android.widget._
import android.support.v7.widget._
import RelativeLayout._
import android.view.{View,ViewGroup}
import ViewGroup.LayoutParams._
import android.app.Activity
import android.graphics.drawable.Drawable

import macroid._
import macroid.FullDsl._
import macroid.contrib.Layouts._
import RuleRelativeLayout.Rule

import tryp.droid.view.ActivityContexts

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

  val ⇹ = Rule(CENTER_HORIZONTAL)

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

  def extractParams(params: Any*) = {
    val rules = ListBuffer[Rule]()
    var width = Width(WRAP_CONTENT)
    var height = Height(WRAP_CONTENT)
    params foreach { param ⇒
      param match {
        case r: Rule ⇒ rules += r
        case w: Width ⇒ width = w
        case h: Height ⇒ height = h
        case _ ⇒
      }
    }
    (width, height, rules)
  }

  def relative(params: Any*) = {
    val (width, height, rules) = extractParams(params: _*)
    lp[RuleRelativeLayout](width.value, height.value, rules: _*)
  }

  def rlp(params: Any*) = relative(params: _*)

  def llp(width: Width = WRAP_CONTENT, height: Height = WRAP_CONTENT, weight:
    Int = 0): Tweak[View] = {
    lp[LinearLayout](width.value, height.value, weight)
  }

  def vlp(width: Width = WRAP_CONTENT, height: Height = WRAP_CONTENT) = {
    lp[ViewGroup](width.value, height.value)
  }

  def tweakSum[A <: View](tweaks: Tweak[A]*): Tweak[A] = {
    tweaks.foldLeft(Tweak[A](a ⇒ Unit))((a, b) ⇒ a + b)
  }

  def foreground(res: Drawable) = Tweak[FrameLayout](_.setForeground(res))

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
