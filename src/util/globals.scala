package tryp.droid.util

class Tapper[A](item: A)
{
    def tap(fun: A ⇒ Unit): A = {
      fun(item)
      item
    }
}

trait Util
extends Control
with MetadataExt
{
}

trait Globals
extends Util
{
  implicit def tapper[A](item: A) = new Tapper(item)
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
}
