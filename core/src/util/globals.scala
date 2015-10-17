package tryp.droid.meta

trait Util
extends AkkaExt
with AndroidExt
with tryp.droid.BundleExt

trait Forward
{
  val Id = tryp.droid.util.Id

  val Tag = tryp.droid.util.Tag

  val TrypKeys = tryp.droid.Keys

  type Id = tryp.droid.util.Id

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
  val AndroidLog = tryp.droid.meta.AndroidLog
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
extends tryp.slick.sync.meta.GlobalsBase
with Util
with Basic
with Forward
with Types

trait Globals
extends tryp.slick.sync.meta.Globals
with Util
with Basic
with Forward
with Types
{
  type Activity = android.app.Activity
  type View = android.view.View
  type ViewGroup = android.view.ViewGroup
  type LayoutInflater = android.view.LayoutInflater
}

trait TrypDroidGlobals
extends Globals
{
  def Fragments = tryp.droid.Classes.fragments
}

trait TrypDroid
extends tryp.droid.ToActionMacroidOps
with tryp.droid.ToUiOps
with tryp.droid.ToUiValidationNelActionOps
