package tryp.droid.res

import android.content.Context

case class Resources(implicit val context: Context)
extends tryp.droid.Basic
with tryp.droid.Preferences
with tryp.droid.view.Themes
{
}

trait ResourceNamespace
{
  def format(ident: String): String
}

case class PrefixResourceNamespace(prefix: String)
extends ResourceNamespace
{
  def format(ident: String) = s"${prefix}_${ident}"
}

object GlobalResourceNamespace
extends ResourceNamespace
{
  def format(ident: String) = ident
}
