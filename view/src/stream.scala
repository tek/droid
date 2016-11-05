package tryp
package droid
package view

import scalaz.concurrent.Task

import scalaz.stream._, Process._
import async.mutable._

import cats.{Applicative, Unapply, Functor}

import core._
import state._

case class StreamIO[A, C](io: IO[A, C])
extends Logging
with IOStrategy
{
  import ApplyKestrel.ops._

  def run: IO[A, C] = io >>- StreamIO.attachSignal[A, C](sig)

  def apply(implicit c: C): A = run(c)

  override def loggerName = List("stream_io")

  lazy val sig = async.signalOf[Option[A]](None)

  def view = sig.continuous flatMap(_.fold[Process[Task, A]](halt)(emit))

  def ios[C2] = view map(a => IO[A, C2](_ => a))

  def v = ViewStream(ios[C])

  def vAs[C2] = ViewStream[A, C2](ios[C2])
}

import PerformIO.ops._

trait StreamIOInstances
{
  implicit lazy val instance_ConsIO_StreamIO =
    new ConsIO[StreamIO] {
      def cons[A, C](fa: StreamIO[A, C])(c: C): A = fa.io(c)
      override def init[A, C](fa: StreamIO[A, C])(c: C): A = fa(c)
      def pure[A, C](run: C => A): StreamIO[A, C] = StreamIO(IO(run))
    }

  implicit def instance_Monad_StreamIO[C]: Monad[StreamIO[?, C]] =
    new Monad[StreamIO[?, C]] {
      def pure[A](a: A) = StreamIO(cats.Monad[IO[?, C]].pure(a))

      def flatMap[A, B](fa: StreamIO[A, C])(f: A => StreamIO[B, C]) = {
        StreamIO(IO(c => f(fa.run(c)).io(c)))
      }

      def tailRecM[A, B](a: A)(f: A => StreamIO[Either[A, B], C])
      : StreamIO[B, C] =
        defaultTailRecM(a)(f)
    }

  implicit def instance_PerformIO_StreamIO =
    new PerformIO[StreamIO] {
      def unsafePerformIO[A, C](fa: StreamIO[A, C])(implicit c: C) =
        Task(fa(c))

      def main[A, C](fa: StreamIO[A, C])(timeout: Duration = Duration.Inf)
      (implicit c: C) = {
        fa.run.main(timeout)
      }
    }
}

trait StreamIOFunctions
{
  class Lifter[C]
  {
    def apply[A](f: C => A) = StreamIO(IO(f))
  }

  def lift[C] = new Lifter[C]
}

object StreamIO
extends Logging
with StreamIOInstances
with StreamIOFunctions
{
  def attachSignal[A, C](sig: Signal[Option[A]])(implicit log: Logger) =
    K[A, C, IO] { a =>
      IO[A, C] { ctx =>
        sig.set(Some(a)) !? "set signal for StreamIO"
        a
      }
    }
}

trait StreamIOViews
extends Views[Context, StreamIO]

object StreamIOViews
extends StreamIOViews

case class ViewStream[A, C](view: Process[Task, IO[A, C]])
{
  def >>-[B >: A](ka: Kestrel[B, C, IO]) = ViewStream(view map(_ >>- ka))

  def desc = toString
}

trait ViewStreamInstances
{
  // TODO product and map are now implemented in Apply et al.
  implicit def instance_Applicative_ViewStream[C] =
    new Applicative[ViewStream[?, C]] {
      def pure[A](a: A) = ViewStream(emit(IO[A, C](c => a)))

      def ap[A, B](ff: ViewStream[A => B, C])
      (fa: ViewStream[A, C]): ViewStream[B, C] =
        map(product(fa, ff)) { case (a, f) => f(a) }

      override def product[A, B](fa: ViewStream[A, C], fb: ViewStream[B, C]) =
      {
        ViewStream {
          fa.view.zip(fb.view) map {
            case (ia, ib) => IO(c => (ia.run(c), ib.run(c)))
          }
        }
      }

      override def map[A, B](fa: ViewStream[A, C])(f: A => B)
      : ViewStream[B, C] = {
        ViewStream(fa.view.map(_.map(f)))
      }
    }
}

object ViewStream
extends ViewStreamInstances

final class IOProcess[F[_, _]: PerformIO, A, C](self: Process[Task, F[A, C]])
(implicit functor: Functor[F[?, C]])
{
  def unit = self map(_.void)
}

trait TOIOProcess
{
  implicit def TOIOProcess[F[_, _]: PerformIO, A, C]
  (proc: Process[Task, F[A, C]])
  (implicit functor: Functor[F[?, C]]) =
    new IOProcess(proc)
}
