package tryp.droid

import scalaz._, Scalaz._

import android.content.res.{Resources ⇒ AResources}

case class InvalidResource(msg: String)
extends java.lang.RuntimeException(msg)

class Resources(implicit val context: Context,
  ns: ResourceNamespace = GlobalResourceNamespace)
extends Preferences
with AppPreferences
{
  type IdTypes = Int with String with Id

  private val global = GlobalResourceNamespace

  lazy val theme = new tryp.droid.view.Theme

  def id[A >: IdTypes](input: A, defType: String = "id"): Int = {
    val res = input match {
      case i: Int ⇒ i
      case i: Id ⇒ i.value
      case name: String ⇒ resources
        .getIdentifier(name, defType, context.getPackageName)
    }
    idO(input, defType) | {
      throw InvalidResource(s"$defType: '$input'")
    }
  }

  def idO[A >: IdTypes](input: A, defType: String = "id"): Option[Int] = {
    val res = input match {
      case i: Int ⇒ i
      case i: Id ⇒ i.value
      case name: String ⇒ resources
        .getIdentifier(name, defType, context.getPackageName)
    }
    if (res == 0) {
      Log.w(s"Resource $defType '$input' resolved to zero!")
      none[Int]
    }
    else res.some
  }

  def integer[A >: IdTypes](_id: A) = res(_id, "integer") { _.getInteger _ }

  def string[A >: IdTypes](_id: A) = res(_id, "string") { _.getString _ }

  def dimen[A >: IdTypes](_id: A) = res(_id, "dimen") { _.getDimension _ }

  def color[A >: IdTypes](_id: A) = res(_id, "color") { _.getColor _ }

  def bool[A >: IdTypes](_id: A) = res(_id, "bool") { _.getBoolean _ }

  def i(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, integer)
  }

  def s(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, string)
  }

  def d(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, dimen)
  }

  def c(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, theme.color)
  }

  def b(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, bool)
  }

  def namespaced[A](
    name: String, suffix: Option[String], getter: (String) ⇒ A) =
  {
    Try(getter(ns.format(name, suffix))) recoverWith {
      case e: InvalidResource if (ns != global) ⇒
        Try(getter(global.format(name, suffix)))
      case e ⇒ Failure(e)
    } recoverWith {
      case e: InvalidResource ⇒
        Failure(InvalidResource(
          s"Couldn't resolve attr '${name}' with suffix '${suffix}' in " +
          s"namespace ${ns}")
        )
      case e ⇒ Failure(e)
    } get
  }

  def xmlId(name: String): Int = id(name, "xml")

  def layoutId(name: String): Int = id(name, "layout")

  def themeId(name: String): Int = id(name, "style")

  def drawableId(name: String): Int = id(name, "drawable")

  def stringId(name: String): Int = id(name, "string")

  def attrId(name: String) = id(name, "attr")

  def xmlIdO(name: String) = idO(name, "xml")

  def layoutIdO(name: String) = idO(name, "layout")

  def themeIdO(name: String) = idO(name, "style")

  def drawableIdO(name: String) = idO(name, "drawable")

  def stringIdO(name: String) = idO(name, "string")

  def attrIdO(name: String) = idO(name, "attr")

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

  override def toString = s"[${prefix}]"
}

object GlobalResourceNamespace
extends ResourceNamespace
{
  def format(ident: String, suf: Option[String] = None) = {
    addSuffix(ident, suf)
  }

  override def toString = "global"
}

trait ResourcesAccess
{
  implicit protected def res(implicit c: Context,
    ns: ResourceNamespace = GlobalResourceNamespace) = new Resources()

  protected def theme(implicit c: Context,
    ns: ResourceNamespace = GlobalResourceNamespace) = {
    res.theme
  }
}

object ResourcesAccess
extends ResourcesAccess
