package tryp.droid.meta

trait Util
extends AkkaExt
with AndroidExt
with ToBundleOps

trait Forward
{
  val Id = tryp.util.Id

  val Tag = tryp.util.Tag

  val TrypKeys = tryp.Keys

  type Id = tryp.util.Id

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
  def GPlus: tryp.GPlusBase = tryp.Classes.plus
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
  def Fragments = tryp.Classes.fragments
}

trait TrypDroid
extends tryp.ToActionMacroidOps
with tryp.ToUiOps
with tryp.ToUiValidationNelActionOps
