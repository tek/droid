package tryp.droid.res

import android.content.Context

case class Resources(implicit val context: Context,
  ns: ResourceNamespace = GlobalResourceNamespace)
extends tryp.droid.Basic
with tryp.droid.Preferences
with tryp.droid.view.Themes
{
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
