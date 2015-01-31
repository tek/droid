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
extends Util
{
  val Env = tryp.droid.meta.Env

  def Log = {
    if (Env.release) {
      tryp.droid.meta.NullLog
    }
    else if (Env.unittest) {
      tryp.droid.meta.StdoutLog
    }
    else {
      tryp.droid.meta.DebugLog
    }
  }

  def log(message: String) = Log.d(message)

  def p[A](item: A): A = {
    (Log.p(item))
    item
  }

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

  val Try = scala.util.Try
  val Success = scala.util.Success
  val Failure = scala.util.Failure
  val Future = scala.concurrent.Future
  val Buffer = scala.collection.mutable.Buffer
  val MMap = scala.collection.mutable.Map

  type ClassTag[A] = scala.reflect.ClassTag[A]
  type Buffer[A] = scala.collection.mutable.Buffer[A]
  type MMap[A, B] = scala.collection.mutable.Map[A, B]
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
  val Fragments = tryp.droid.Classes.fragments
}
