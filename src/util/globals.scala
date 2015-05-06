package tryp.droid.util

trait Util
extends Control
with MetadataExt
with OptionExt
with StringOpsExt
with SeqExt
with AkkaExt
with AndroidExt
with JodaExt
{
}

trait GlobalsBase
extends tryp.core.meta.Globals
with Util
{
  val Env = tryp.droid.meta.Env

  override def Log = {
    if (Env.release) {
      tryp.droid.meta.InternalLog
    }
    else if (Env.unittest) {
      tryp.core.meta.StdoutLog
    }
    else {
      tryp.droid.meta.DebugLog
    }
  }

  def log(message: String) = Log.d(message)

  val layouts = tryp.droid.res.Layouts

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

  def GPlus: tryp.droid.GPlusBase = tryp.droid.Classes.plus
}

trait Globals
extends GlobalsBase
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
