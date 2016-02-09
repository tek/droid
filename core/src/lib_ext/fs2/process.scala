package tryp
package droid

import concurrent.duration.FiniteDuration

import java.util.concurrent._

import ZS._
import Process._
import scalaz.Liskov.<~<
import scalaz.concurrent.Strategy
import scalaz.syntax.std.option._

final class TaskProcessOps[O](self: Process[Task, O])
extends ToTaskOps
{
  def headOption = self |> process1.awaitOption

  def headOr[O2 >: O](alt: ⇒ O2) =
      headOption
      .map(_ getOrElse alt)

  def timedHeadOr[O2 >: O](timeout: FiniteDuration, alt: ⇒ O2) = {
    implicit val sched = Strategy.DefaultTimeoutScheduler
    (time.sleep(timeout) ++ emit(alt))
      .merge(self)
      .take(1)
  }

  def value = self.runLast.map(_.toMaybe)

  def valueOr(alt: ⇒ O) = self.runLast.map(_ | alt)
}

trait ToProcessOps0
{
  implicit def ToTaskProcessOps[A](v: Process[Task, A]) =
    new TaskProcessOps(v)
}

final class TaskProcessEffectOps[O](self: Process[Task, O])
(implicit log: Logger)
extends ToTaskOps
with ToProcessOps0
{
  def infraFork(desc: String)(implicit x: ExecutorService) = {
    self.run.infraFork(desc)
  }

  def infraRunFor(desc: String, timeout: Duration) = {
    self.run.infraRunFor(desc, timeout)
  }

  def infraRunShort(desc: String)(implicit timeout: Duration = 5 seconds) = {
    self.run.infraRunShort(desc)
  }

  def infraValueShortOr(alt: ⇒ O)(desc: String)
  (implicit timeout: Duration = 5 seconds) =
  {
    self.valueOr(alt).infraRunShort(desc) | alt
  }

  def !?(desc: String)(implicit timeout: Duration = 5 seconds) =
    infraRunShort(desc)

  def infraRunLogFork(desc: String)(implicit x: ExecutorService) = {
    self.runLog.infraFork(desc)
  }

  def infraRunLogFor(desc: String, timeout: Duration) = {
    self.runLog.infraRunFor(desc, timeout)
  }

  def infraRunLastFor(desc: String, timeout: Duration) = {
    self.runLast.infraRunFor(desc, timeout).toOption.flatten
  }

  def peek()(implicit timeout: Duration = 5 seconds) = self.runLog.peek()

  def logged(desc: String): Process[Task, O] = {
    self observe sink.lift(m ⇒ Task(log.trace(s"$desc delivering $m")))
  }
}

final class WriterOps[F[_], W, O](val self: Writer[F, W, O])
extends AnyVal
{
  def forkO(sink: Sink[F, O]) = self.observeO(sink).stripO

  def forkW(sink: Sink[F, W]) = self.observeW(sink).stripW

  def merge[W2 >: W](implicit ev: O <~< W2): Process[F, W2] = self map(_.merge)

  def mergeO[W2 >: W](implicit ev: O <~< W2): Writer[F, Nothing, W2] =
    self map(a ⇒ \/-(a.merge))

  def mergeW[W2 >: W](implicit ev: O <~< W2): Writer[F, W2, Nothing] =
    self map(a ⇒ -\/(a.merge))
}

final class ProcessOps[F[_], O](val self: Process[F, O])
extends AnyVal
{
  def separate(f: O ⇒ Boolean): Writer[F, O, O] = {
    self map { a ⇒
      if (f(a)) \/-(a)
      else -\/(a)
    }
  }

  def separateMap[O2](f: O ⇒ Boolean)(g: O ⇒ O2): Writer[F, O2, O2] = {
    separate(f) map(_.bimap(g, g))
  }
}

object SignalOps
{
  def signalSetter[A] = process1.lift[A, Signal.Set[A]](Signal.Set(_))
}

final class SignalOps[A](val self: Signal[A])
extends AnyVal
{
  def pipeIn = self.sink.pipeIn(SignalOps.signalSetter[A])

  def setter(value: A) = Process.emit(value) observe pipeIn
}

trait ToProcessOps
extends ToProcessOps0
{
  implicit def ToTaskProcessEffectOps[A](v: Process[Task, A])
  (implicit log: Logger) =
    new TaskProcessEffectOps(v)

  implicit def ToWriterOps[F[_], W, O](v: Writer[F, W, O]) = new WriterOps(v)

  implicit def ToProcessOps[F[_], O](v: Process[F, O]) = new ProcessOps(v)

  implicit def ToSignalOps[A](sig: Signal[A]) = new SignalOps(sig)
}
