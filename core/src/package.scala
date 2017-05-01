package tryp
package droid
package core

@exportTypes(android.app.Activity, android.view.View, android.view.ViewGroup, android.content.Context,
  android.os.Bundle, android.content.Intent, android.app.Fragment, android.widget.FrameLayout,
  android.widget.LinearLayout, android.widget.RelativeLayout, android.widget.TextView, android.widget.EditText)
trait Types
{
  def TrypKeys = droid.core.Keys

  val WRAP_CONTENT = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
  val MATCH_PARENT = android.view.ViewGroup.LayoutParams.MATCH_PARENT
}

@exportVals(GlobalResourceNamespace)
trait Vals

trait All
extends syntax.std.ToBundleOps

@exportNames(Resources, ResourcesAccess, ResourceNamespace, PrefixResourceNamespace, RId, ResId)
trait Exports
extends Types
with Vals
{
  val Tag = tryp.droid.core.Tag
}

@integrate(tryp)
object `package`
extends Types
