package tryp.droid.tweaks

import scala.reflect.ClassTag
import scala.language.postfixOps

import android.graphics.{Point, Color}
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.InputType
import android.util.TypedValue
import android.view.View.MeasureSpec
import android.view.inputmethod.EditorInfo
import android.view.{WindowManager, View, ViewGroup}
import android.widget._

import macroid._
import macroid.FullDsl._

/**
 * @author pfnguyen
 */
trait Pfn {
  import ViewGroup.LayoutParams._

  def margin(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0,
    all: Int = -1) = Tweak[View] { v ⇒
    v.getLayoutParams match {
      case m: ViewGroup.MarginLayoutParams ⇒
        if (all >= 0) {
          m.topMargin = all
          m.bottomMargin = all
          m.rightMargin = all
          m.leftMargin = all
        } else {
          m.topMargin = top
          m.bottomMargin = bottom
          m.rightMargin = right
          m.leftMargin = left
        }
      case _ ⇒
    }
  }

  def bgres(resid: Int) = Tweak[View](_.setBackgroundResource(resid))

  def bg(drawable: Drawable) = Tweak[View](_.setBackgroundDrawable(drawable))
  def bg(color: Int) = Tweak[View](_.setBackgroundColor(color))
  def bg(color: String) = Tweak[View](
    _.setBackgroundColor(Color.parseColor(color)))

  def inputType(types: Int) = Tweak[TextView](_.setInputType(types))

  def hidden = Tweak[View](_.setVisibility(View.INVISIBLE))

  def tweak[A <: View,B](f: A ⇒ B) = Tweak[A](a ⇒ f(a))

  private lazy val primitiveMap: Map[Class[_],Class[_]] = Map(
    classOf[java.lang.Integer]   -> java.lang.Integer.TYPE,
    classOf[java.lang.Double]    -> java.lang.Double.TYPE,
    classOf[java.lang.Short]     -> java.lang.Short.TYPE,
    classOf[java.lang.Float]     -> java.lang.Float.TYPE,
    classOf[java.lang.Character] -> java.lang.Character.TYPE,
    classOf[java.lang.Byte]      -> java.lang.Byte.TYPE
  )
  abstract class LpRelation[V <: ViewGroup, LP <: ViewGroup.LayoutParams : ClassTag] {
    def lpType = implicitly[ClassTag[LP]].runtimeClass
    def lp(args: Any*) = lpType.getConstructor(
      args map { a ⇒
        val c = a.getClass
        primitiveMap.getOrElse(c, c)
      }:_*).newInstance(args map (_.asInstanceOf[AnyRef]): _*).asInstanceOf[LP]
  }
  implicit object LLRelation extends LpRelation[LinearLayout, LinearLayout.LayoutParams]
  implicit object TRRelation extends LpRelation[TableRow, TableRow.LayoutParams]

  def lp2[V <: ViewGroup,LP <: ViewGroup.LayoutParams, C](args: Any*)
  (callback: LP ⇒ C)
  (implicit r: LpRelation[V,LP]) = tweak {
    v: View ⇒
      val lp = r.lp(args: _*)
      callback(lp)
      v.setLayoutParams(lp)
  }
}

object Pfn extends Pfn

class SquareImageButton(c: Context) extends ImageButton(c) {
  override def onMeasure(mw: Int, mh: Int) = {
    val w = MeasureSpec.getSize(mw)
    val h = MeasureSpec.getSize(mh)
    val m = if (w > h) mw else mh
    super.onMeasure(m, m)
  }
}
