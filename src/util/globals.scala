package tryp.droid.util

trait Util
extends Control
with MetadataExt
with OptionExt
with StringOpsExt
with tryp.droid.AkkaExt
{
}

trait Globals
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

  val Ui = macroid.Ui
}
