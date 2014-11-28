package tryp.droid.tweaks

import scala.collection.mutable.ListBuffer

import android.widget._
import android.widget.RelativeLayout._
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams._
import android.view.View
import android.app.Activity

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

  def extractParams(params: Any*) = {
    val rules = ListBuffer[Rule]()
    var width = Width(WRAP_CONTENT)
    var height = Height(WRAP_CONTENT)
    params foreach { param =>
      param match {
        case r: Rule => rules += r
        case w: Width => width = w
        case h: Height => height = h
        case _ =>
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
    Int = 0) = {
    lp[LinearLayout](width.value, height.value, weight)
  }

  def vlp(width: Width = WRAP_CONTENT, height: Height = WRAP_CONTENT) = {
    lp[ViewGroup](width.value, height.value)
  }

  def tweakSum[A <: View](tweaks: Tweak[A]*): Tweak[A] = {
    tweaks.foldLeft(Tweak[A](a => Unit))((a, b) => a + b)
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
