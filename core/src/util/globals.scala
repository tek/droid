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

  type Context = android.content.Context
  type Bundle = android.os.Bundle
  type Intent = android.content.Intent
  type Fragment = android.app.Fragment
  type FrameLayout = android.widget.FrameLayout
  type RelativeLayout = android.widget.RelativeLayout

  type Ui[+A] = macroid.Ui[A]
  type Tweak[-A <: android.view.View] = macroid.Tweak[A]
  type Snail[-A <: android.view.View] = macroid.Snail[A]

  val Ui = macroid.Ui
  val Tweak = macroid.Tweak
}

trait Types
{
  type AnyUi = macroid.Ui[_]
}

trait Basic
{
  def GPlus: tryp.droid.GPlusBase = tryp.droid.Classes.plus
}

trait GlobalsBase
extends slick.sync.meta.GlobalsBase
with Util
with Basic
with Types

trait Implicits
extends droid.ViewInstances
with droid.ToViewOps
with droid.ToIntentOps
with droid.ToTaskOps

trait Globals
extends tryp.slick.sync.meta.Globals
with Util
with Basic
with Forward
with Types
with Implicits
{
  type Activity = android.app.Activity
  type View = android.view.View
  type ViewGroup = android.view.ViewGroup
  type LayoutInflater = android.view.LayoutInflater

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
