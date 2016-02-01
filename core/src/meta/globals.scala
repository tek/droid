package tryp
package droid
package core
package meta

trait AndroidTypes
{
  type Activity = android.app.Activity
  type View = android.view.View
  type ViewGroup = android.view.ViewGroup
  type Context = android.content.Context
  type Bundle = android.os.Bundle

  type Ui[+A] = macroid.Ui[A]
  val Ui = macroid.Ui
}

trait Globals
extends tryp.meta.Globals
with Exports

trait Exports
extends ToTaskOps
with ToProcessOps
with AndroidTypes
{
  type Resources = core.Resources
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
