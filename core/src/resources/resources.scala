package tryp
package droid
package core

import reflect.macros.blackbox

import android.content.res.{Resources => AResources}

import simulacrum._

@typeclass trait ResId[A]
{
  def id(a: A)(implicit res: ResourcesInternal) = typed(a, "id")

  def typed(a: A, defType: String)
  (implicit res: ResourcesInternal): Throwable Xor RId
}

object ResId
{
  implicit val intResId = new ResId[Int] {
    def typed(a: Int, defType: String)(implicit res: ResourcesInternal) = {
      if (a > 0) Xor.right(a: RId)
      else Xor.left(new Throwable(s"Bad Int id for type '$defType': $a"))
    }
  }

  implicit val idResId = new ResId[RId] {
    def typed(a: RId, defType: String)(implicit res: ResourcesInternal) = {
      intResId.typed(a.value, defType)
    }
  }

  implicit def stringResId = new ResId[String] {
    def typed(a: String, defType: String)(implicit res: ResourcesInternal) = {
      res.identifier(defType, a) map(a => a: RId)
    }
  }
}

case class InvalidResource(msg: String)
extends java.lang.RuntimeException(msg)

trait ResourcesInternal
{
  def identifier(defType: String, input: String): Throwable Xor Int

  def resourceName(id: Int): String

  def res[B](id: Int, defType: String)
  (callback: AResources => Int => B): Throwable Xor B
}

object ResourcesInternal
{
  // implicit def materialize(implicit con: Context): ResourcesInternal =
  //   macro ResourcesInternalMacros.materializeFromContext

  implicit def fromContext(implicit con: Context): ResourcesInternal = {
    new AndroidResourcesInternal
  }
}

class ResourcesInternalMacros(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._

  def materializeFromContext(con: c.Expr[Context]) =
    q"new AndroidResourcesInternal($con)"
}

class AndroidResourcesInternal(implicit val context: Context)
extends ResourcesInternal
{
  def resources = context.getResources

  def identifier(defType: String, input: String): Throwable Xor Int = {
    val id = resources.getIdentifier(input, defType, context.getPackageName)
    if (id > 0) Xor.right(id)
    else Xor.left(
      new Throwable(s"no identifier found for $defType id '$input'"))
  }

  def resourceName(_id: Int) = resources.getResourceName(_id)

  def res[B](id: Int, defType: String)
  (callback: AResources => Int => B): Throwable Xor B = {
    Xor.catchNonFatal(callback(resources)(id)) recoverWith {
      case e: AResources.NotFoundException =>
        val msg = s"no resource found for $defType id '$id'"
        Log.e(msg)
        Xor.left(new Throwable(msg))
    }
  }
}

class Resources(implicit internal: ResourcesInternal,
  val theme: Theme, ns: ResourceNamespace = GlobalResourceNamespace)
{
  import ResId.ops._

  private val global = GlobalResourceNamespace

  def id[A: ResId](input: A) = typedId(input, "id")

  def typedId[A: ResId](input: A, defType: String): Throwable Xor RId = {
    input.typed(defType)
  }

  def res[A: ResId, B](id: A, defType: String)
  (callback: AResources => (Int => B)): Throwable Xor B =
    for {
      id <- typedId(id, defType) leftMap(a => new Throwable(a))
      r <- internal.res(id, defType)(callback)
    } yield r

  def integer[A: ResId](_id: A) = res(_id, "integer")(_.getInteger _)

  def string[A: ResId](_id: A) = res(_id, "string")(_.getString _)

  def dimen[A: ResId](_id: A) = res(_id, "dimen")(_.getDimension _)

  def color[A: ResId](_id: A) = res(_id, "color")(_.getColor _)

  def bool[A: ResId](_id: A) = res(_id, "bool")(_.getBoolean _)

  def i(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, integer[String])
  }

  def s(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, string[String])
  }

  def d(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, dimen[String])
  }

  def c(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, theme.color)
  }

  def b(name: String, suffix: Option[String] = None) = {
    namespaced(name, suffix, bool[String])
  }

  def namespaced[A](name: String, suffix: Option[String], getter: String => A) =
  {
    Try(getter(ns.format(name, suffix))) recoverWith {
      case e: InvalidResource if (ns != global) =>
        Try(getter(global.format(name, suffix)))
      case e => Failure(e)
    } recoverWith {
      case e: InvalidResource =>
        Failure(InvalidResource(
          s"Couldn't resolve attr '${name}' with suffix '${suffix}' in " +
          s"namespace ${ns}")
        )
      case e => Failure(e)
    } get
  }

  def xmlId(name: String) = typedId(name, "xml")

  def layoutId(name: String) = typedId(name, "layout")

  def themeId(name: String) = typedId(name, "style")

  def drawableId(name: String) = typedId(name, "drawable")

  def stringId(name: String) = typedId(name, "string")

  def attrId(name: String) = typedId(name, "attr")

  def resourceName(id: Int) = internal.resourceName(id)
}

object Resources
{
  implicit def fromContext(implicit con: Context) = new Resources
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
    ns: ResourceNamespace = GlobalResourceNamespace) = {
      implicit val ri = new AndroidResourcesInternal
      implicit val t = new Theme()(new AndroidThemeInternal)
      new Resources
    }

  protected def theme(implicit c: Context,
    ns: ResourceNamespace = GlobalResourceNamespace) = {
    res.theme
  }
}

object ResourcesAccess
extends ResourcesAccess
