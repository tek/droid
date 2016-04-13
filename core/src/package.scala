package tryp
package droid
package core

@exportNames(android.app.Activity, android.view.View, android.view.ViewGroup,
  android.content.Context, android.os.Bundle, android.content.Intent,
  android.app.Fragment, android.widget.FrameLayout,
  android.widget.RelativeLayout, android.widget.TextView,
  android.widget.EditText
  )
trait Decls
{
  val TrypKeys = droid.core.Keys
  
  type SlickEffect = _root_.slick.dbio.Effect
  type SlickAction[A, E <: SlickEffect] =
    _root_.slick.dbio.DBIOAction[A, _root_.slick.dbio.NoStream, E]
}

trait All
extends syntax.std.ToBundleOps
with ToTaskOps
with ToInfraTaskOps
with ToProcessOps

@exportNames(Resources, ResourcesAccess, ResourceNamespace,
  PrefixResourceNamespace, GlobalResourceNamespace, RId, Tag, ResId)
trait Exports

object `package`
extends tryp.slick.meta.Globals
with Decls
