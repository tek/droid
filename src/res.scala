package tryp.droid.res

import android.content.res.{Resources ⇒ AResources}

case class Resources(implicit val context: Context,
  ns: ResourceNamespace = GlobalResourceNamespace)
extends tryp.droid.Preferences
{
  type IdTypes = Int with String with Id

  lazy val theme = new tryp.droid.view.Theme

  def id[A >: IdTypes](input: A, defType: String = "id"): Int = {
    val res = input match {
      case i: Int ⇒ i
      case i: Id ⇒ i.value
      case name: String ⇒ resources
        .getIdentifier(name, defType, context.getPackageName)
    }
    res.tapIfEquals(0) { i ⇒
      Log.e(s"Resource ${defType} '${input}' resolved to zero!")
    }
  }

  def integer[A >: IdTypes](_id: A) = res(_id, "integer") { _.getInteger _ }

  def string[A >: IdTypes](_id: A) = res(_id, "string") { _.getString _ }

  def dimen[A >: IdTypes](_id: A) = res(_id, "dimen") { _.getDimension _ }

  def color[A >: IdTypes](_id: A) = res(_id, "color") { _.getColor _ }

  def i(name: String, suffix: Option[String] = None) = {
    integer(ns.format(name, suffix))
  }

  def s(name: String, suffix: Option[String] = None) = {
    string(ns.format(name, suffix))
  }

  def d(name: String, suffix: Option[String] = None) = {
    dimen(ns.format(name, suffix))
  }

  def c(name: String, suffix: Option[String] = None) = {
    color(ns.format(name, suffix))
  }

  def xmlId(name: String): Int = id(name, "xml")

  def layoutId(name: String): Int = id(name, "layout")

  def themeId(name: String): Int = id(name, "style")

  def drawableId(name: String): Int = id(name, "drawable")

  def stringId(name: String): Int = id(name, "string")

  def resourceName(_id: Int) = resources.getResourceName(_id)

  def resources = context.getResources

  def res[A >: IdTypes, B](_id: A, defType: String)(
    callback: AResources ⇒ (Int ⇒ B)
  ): B = {
    try {
      callback(resources)(id(_id, defType))
    }
    catch {
      case e: AResources.NotFoundException ⇒ {
        val msg = s"No ${defType} with identifier '${_id}' found"
        throw new AResources.NotFoundException(msg)
      }
    }
  }
}

trait ResourceNamespace
{
  def format(ident: String, suf: Option[String] = None): String

  def addSuffix(s: String, suf: Option[String]) = {
    suf map { s + "_" + _ } getOrElse s
  }
}

case class PrefixResourceNamespace(prefix: String)
extends ResourceNamespace
{
  def format(ident: String, suf: Option[String] = None) = {
    addSuffix(s"${prefix}_${ident}", suf)
  }
}

object GlobalResourceNamespace
extends ResourceNamespace
{
  def format(ident: String, suf: Option[String] = None) = {
    addSuffix(ident, suf)
  }
}
