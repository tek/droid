package tryp
package droid.meta

trait Util
extends AkkaExt
with AndroidExt
with droid.BundleExt

trait Forward
extends slick.sync.Exports
{
  val TrypKeys = tryp.droid.Keys

  type Intent = android.content.Intent
  type Fragment = android.app.Fragment
  type FrameLayout = android.widget.FrameLayout
  type RelativeLayout = android.widget.RelativeLayout

  type Tweak[-A <: android.view.View] = macroid.Tweak[A]
  type Snail[-A <: android.view.View] = macroid.Snail[A]

  val Tweak = macroid.Tweak
}

trait Types
{
  type AnyUi = macroid.Ui[_]
}

trait GlobalsBase
extends slick.sync.meta.GlobalsBase
with Util
with Types

trait Implicits
extends droid.ViewInstances
with droid.ToViewOps
with droid.ToIntentOps
with droid.ToSearchViewOps

trait Globals
extends tryp.slick.sync.meta.Globals
with droid.view.meta.Globals
with Util
with Forward
with Types
with Implicits
with droid.state.Exports
with droid.core.meta.AndroidTypes
{
  val AndroidLog = tryp.droid.meta.AndroidLog
}

trait TrypDroidGlobals
extends Globals
{
  def Fragments = tryp.droid.Classes.fragments
}

trait TrypDroid
extends droid.ToActionMacroidOps
with droid.ToUiOps
with droid.ToUiValidationNelActionOps
