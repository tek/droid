package tryp.droid

import android.content.Context

import tryp.droid.util.Id

trait Basic {
  implicit def context: Context

  def id(input: Any, defType: String = "id"): Int = {
    input match {
      case i: Int => i
      case i: Id => i
      case name: String => resources
        .getIdentifier(name, defType, context.getPackageName)
      case _ => throw new IllegalArgumentException
    }
  }

  def integer(_id: Any): Int = resources.getInteger(id(_id, "integer"))

  def string(_id: Any): String = resources.getString(id(_id, "string"))

  def dimen(_id: Any) = resources.getDimension(id(_id, "dimen"))

  def xmlId(name: String): Int = id(name, "xml")

  def layoutId(name: String): Int = id(name, "layout")

  def themeId(name: String): Int = id(name, "style")

  def drawableId(name: String): Int = id(name, "drawable")

  def resources = context.getResources
}
