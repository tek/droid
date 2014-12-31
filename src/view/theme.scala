package tryp.droid.view

import android.app.{Activity ⇒ AActivity}
import android.content.res.{TypedArray,ColorStateList}
import android.graphics.drawable.Drawable

class Theme(implicit val context: Context)
extends tryp.droid.Basic
{

  def drawable(name: String): Drawable = {
    styledAttribute(name, _.getDrawable(0))
  }

  def color(name: String): Int = {
    styledAttribute(name, _.getColor(0, 0))
  }

  def dimension(name: String, defValue: Float = 10) = {
    styledAttribute(name, _.getDimension(0, defValue))
  }

  def colorStateList(name: String): ColorStateList = {
    styledAttribute(name, _.getColorStateList(0))
  }

  def styledAttribute[T](name: String, getter: TypedArray ⇒ T): T = {
    styledAttributes(List(name), getter)
  }

  def styledAttributes[T](names: List[String], getter: TypedArray ⇒ T): T = {
    val arr = names.map(refAttr(_)).toArray
    val attrs = context.obtainStyledAttributes(arr)
    try {
      getter(attrs)
    } finally {
      attrs.recycle
    }
  }

  def refAttr(name: String) = res.id(name, "attr")
}
