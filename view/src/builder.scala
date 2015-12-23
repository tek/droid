package tryp
package droid
package view

import reflect.macros.whitebox

import scalaz.{Tree ⇒ STree, _}, Scalaz._, concurrent._, stream._, Process._
import async.mutable._

@core.IOBase
object IOBase

object IOB
extends CachedPool
{
  def attachSignal[A](sig: Signal[Maybe[A]])(implicit log: Logger) =
    iota.kestrel[A, Unit] { a ⇒
      sig.set(a.just).infraRunShort("set signal for IOB")
    }
}

case class IOB[A](create: Context ⇒ iota.IO[A])
extends IOT[A]
with Logging
with IOStrategy
{
  def cachedPool = IOB

  override val loggerName = Some("iob")

  def >>=[B](f: A ⇒ iota.IO[B]): IOB[B] = IOB(create >=> f)

  override def reify(c: Context): iota.IO[A] = {
    super.reify(c) >>= IOB.attachSignal(sig)
  }

  lazy val sig = async.signalOf[Maybe[A]](Maybe.Empty())

  def vs = sig.continuous flatMap(_.cata(emit, halt))

  def v = vs |> await1
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
