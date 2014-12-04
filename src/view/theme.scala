package tryp.droid.view

import android.app.{Activity => AActivity}
import android.content.res.{TypedArray,ColorStateList}
import android.graphics.drawable.Drawable

class Theme(implicit impAct: AActivity)
extends tryp.droid.view.Activity
{
  override implicit def activity = impAct

  def drawable(name: String): Drawable = {
    styledAttribute(name, _.getDrawable(0))
  }

  def color(name: String): Int = {
    styledAttribute(name, _.getColor(0, 0))
  }

  def colorStateList(name: String): ColorStateList = {
    styledAttribute(name, _.getColorStateList(0))
  }

  def styledAttribute[T](name: String, getter: TypedArray => T): T = {
    styledAttributes(List(name), getter)
  }

  def styledAttributes[T](names: List[String], getter: TypedArray => T): T = {
    val arr = names.map(refAttr(_)).toArray
    val attrs = activity.obtainStyledAttributes(arr)
    try {
      getter(attrs)
    } finally {
      attrs.recycle
    }
  }

  def refAttr(name: String) = id(name, "attr")
}
