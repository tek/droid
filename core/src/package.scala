package tryp
package droid
package core

@exportNames(android.app.Activity, android.view.View, android.view.ViewGroup,
  android.content.Context, android.os.Bundle, android.content.Intent,
  android.app.Fragment, android.widget.FrameLayout,
  android.widget.LinearLayout, android.widget.RelativeLayout,
  android.widget.TextView, android.widget.EditText
  )
trait Names
{
  def TrypKeys = droid.core.Keys

  type SlickEffect = _root_.slick.dbio.Effect
  type SlickAction[A, E <: SlickEffect] =
    _root_.slick.dbio.DBIOAction[A, _root_.slick.dbio.NoStream, E]

  val WRAP_CONTENT = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
  val MATCH_PARENT = android.view.ViewGroup.LayoutParams.MATCH_PARENT
}

trait All
extends syntax.std.ToBundleOps

@exportNames(Resources, ResourcesAccess, ResourceNamespace,
  PrefixResourceNamespace, RId, ResId)
trait Exports
extends Names
{
  val Tag = tryp.droid.core.Tag
  val ResId = tryp.droid.core.ResId
  val RId = tryp.droid.core.RId
  val GlobalResourceNamespace = tryp.droid.core.GlobalResourceNamespace
  val Resources = tryp.droid.core.Resources
}

@integrate(slick)
object `package`
extends Names
