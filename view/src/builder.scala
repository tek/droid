package tryp
package droid
package view

import reflect.macros.whitebox

import scalaz.{Tree ⇒ STree, _}, Scalaz._, concurrent._, stream._, Process._

import android.content.Context
import android.view.{View, ViewGroup}

import org.log4s.Logger

@core.IOBase
object IOBase

case class IOB[A](create: Context ⇒ iota.IO[A])
extends IOT[A]
{
  def >>=[B](f: A ⇒ iota.IO[B]): IOB[B] = IOB(create >=> f)

  def perform()(implicit c: Context, log: Logger): A = {
    super.perform() unsafeTap { v ⇒
      sig.set(v.just).infraRun("set signal for IOT")
    }
  }

  val sig = async.signalOf[Maybe[A]](Maybe.Empty())

  def v = (sig.discrete |> await1) flatMap(_.cata(emit, halt))
}

trait ExtViews
{
  def w[A]: IOB[A] = macro ExtViewM.w[A]

  def l[A <: ViewGroup](vs: IOT[_ <: View]*): IOB[A] =
    macro ExtViewM.l[A]
}

class ExtViewM(val c: whitebox.Context)
extends ViewMBase
{
  import c.universe._

  def w[A: c.WeakTypeTag]: c.Expr[IOB[A]] = {
    c.Expr[IOB[A]] {
      q"""
      tryp.droid.view.IOB(iota.w[${weakTypeOf[A]}](_))
      """
    }
  }

  def l[A <: ViewGroup: c.WeakTypeTag](vs: c.Expr[IOT[_ <: View]]*)
  : c.Expr[IOB[A]] = {
    lImpl(reify(IOB.apply[A] _))(vs)
  }
}
