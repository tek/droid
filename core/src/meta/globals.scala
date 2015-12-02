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
with ToTaskOps
with ToProcessOps
with AndroidTypes
