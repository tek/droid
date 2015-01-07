package tryp.droid.util

trait Util
extends Control
with MetadataExt
with OptionExt
with StringOpsExt
with tryp.droid.AkkaExt
{
}

trait GlobalsBase
extends Util
{
  val Env = tryp.droid.util.Env

  def Log = {
    if (Env.release) tryp.droid.util.NullLog else tryp.droid.util.Log
  }

  def log(message: String) = Log.d(message)
  def p[A](item: A): A = {
    (Log.p(item))
    item
  }

  val Debug = tryp.droid.util.Debug

  val layouts = tryp.droid.res.Layouts

  val fragments = tryp.droid.res.Fragments

  val Id = tryp.droid.util.Id

  val Tag = tryp.droid.util.Tag

  type Id = tryp.droid.util.Id

  type Context = android.content.Context
  type Bundle = android.os.Bundle
  type Intent = android.content.Intent
  type Fragment = android.app.Fragment

  type Ui[+A] = macroid.Ui[A]

  val Ui = macroid.Ui

  val Try = scala.util.Try
}

trait Globals
extends GlobalsBase
{
  type Activity = android.app.Activity
  type View = android.view.View
  type ViewGroup = android.view.ViewGroup
  type LayoutInflater = android.view.LayoutInflater
}
