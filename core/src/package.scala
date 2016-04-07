package tryp
package droid
package core

import _root_.slick.dbio.NoStream

trait Decls
{
  val TrypKeys = core.Keys
  type Activity = android.app.Activity
  type View = android.view.View
  type ViewGroup = android.view.ViewGroup
  type Context = android.content.Context
  type Bundle = android.os.Bundle
  type Intent = android.content.Intent
  type Fragment = android.app.Fragment
  type FrameLayout = android.widget.FrameLayout
  type RelativeLayout = android.widget.RelativeLayout
  type TextView = android.widget.TextView
  type EditText = android.widget.EditText
  type SlickEffect = _root_.slick.dbio.Effect
  type SlickAction[A, E <: SlickEffect] =
    _root_.slick.dbio.DBIOAction[A, NoStream, E]
}

trait Exports
extends syntax.std.ToBundleOps
with ToTaskOps
with ToInfraTaskOps
with ToProcessOps

trait ExportDecls
extends Decls
{
  type Resources = core.Resources
  val Resources = core.Resources
  type ResourcesAccess = core.ResourcesAccess
  type ResourceNamespace = core.ResourceNamespace
  type PrefixResourceNamespace = core.PrefixResourceNamespace
  val GlobalResourceNamespace = core.GlobalResourceNamespace
  val PrefixResourceNamespace = core.PrefixResourceNamespace
  type RId = core.RId
  val RId = core.RId
  val Tag = core.Tag
  type ResId[A] = core.ResId[A]
}

object `package`
extends tryp.slick.meta.Globals
with Decls
