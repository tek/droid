package tryp
package droid
package view

import reflect.macros.blackbox

import cats._
import cats.data._
import Func._

import scalaz.{Tree ⇒ STree, _}, Scalaz._, concurrent._, stream._, Process._
import async.mutable._

object FreeIO
extends Logging
{
  def attachSignal[A](sig: Signal[Maybe[A]])(implicit log: Logger) =
    iota.kestrel[A, Unit] { a ⇒
      sig.set(a.just).infraRunShort("set signal for FreeIO")
    }

  implicit def freeIoBuilder(implicit log: Logger = log) =
    new IOBuilder[FreeIO]
    {
      override def reify[A](fa: FreeIO[A])(c: Context): iota.IO[A] = {
        super.reify(fa)(c) >>= FreeIO.attachSignal(fa.sig)
      }
      def pure[A](f: IOCtor[A]) = FreeIO(f)

      def ctor[A](fa: FreeIO[A]) = fa.ctor
    }
}

case class FreeIO[A](ctor: IOCtor[A])
extends Logging
with IOStrategy
{

  override val loggerName = Some("freeio")

  lazy val sig = async.signalOf[Maybe[A]](Maybe.Empty())

  def view = sig.continuous flatMap(_.cata(emit, halt))

  def io = view map(iota.IO(_))

  def v = ViewStream(io)
}

case class ViewStream[A](view: Process[Task, iota.IO[A]]) {
  def >>=[B](f: A ⇒ iota.IO[B]): ViewStream[B] = ViewStream(view map(_ >>= f))

  def unsafePerformIOMain(timeout: Duration = Duration.Inf) =
    view
      .take(1)
      .map(_.performMain())
      .map(scala.concurrent.Await.result(_, timeout))
}

trait ExtViews
{
  def w[A <: View]: FreeIO[A] = macro ViewM.w[A, FreeIO]

  def l[A <: ViewGroup](inner: Any): FreeIO[A] = macro ViewM.l[A, FreeIO]
}
