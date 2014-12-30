package tryp.droid.res

import scala.collection.mutable.{Map => MMap}

import android.content.Context

abstract class Style
{
  Themes.add(this)

  def name: String
}

object Style
{
  var resourcesInstance: Resources = null

  def resources(implicit c: Context) = {
    if (resourcesInstance == null) {
      resourcesInstance = new Resources()
    }
    resourcesInstance
  }

  def apply(implicit c: Context) = {
    Themes.get(resources.pref("theme"))
  }
}

abstract class Default
extends Style
{
  def button = {

  }
}

object Default
extends Default
{
  def name = "default"
}

class DarkGrin
extends Default
{
  def name = "dark_grin"
}

class LightBlu
extends Default
{
  def name = "light_blu"
}

object Themes
{
  val themes = MMap[String, Style]()

  def add(theme: Style) {
    themes(theme.name) = theme
  }

  def get(name: String) = {
    themes.get(name) getOrElse Default
  }
}
