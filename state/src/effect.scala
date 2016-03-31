package tryp
package droid
package state

import scala.annotation.implicitNotFound

import scalaz._, Scalaz._

import shapeless._
import shapeless.tag.@@

import simulacrum._

import view.core._
import core._

import Process._

trait ToProcess[F[_], G[_]]
{
  def proc[A](fa: G[A]): Process[F, A]
}

object ToProcess
{
  implicit def anyActionToProcess(implicit ec: EC, dbi: DbInfo) =
    new ToProcess[Task, AnyAction] {
      def proc[A](aa: AnyAction[A]) = aa.proc
    }

  implicit def taskToProcess =
    new ToProcess[Task, Task] {
      def proc[A](ta: Task[A]) = Process.eval(ta)
    }
}

trait ToProcessSyntax
{
  implicit class ToToProcess[A, F[_], G[_]](ga: G[A])
  (implicit tp: ToProcess[F, G])
  {
    def proc = tp.proc(ga)
  }
}

@implicitNotFound("no StateEffect[${A}] defined")
@typeclass trait StateEffect[A]
{
  def stateEffect(a: A): Effect
}

trait ToStateEffect
{
  implicit def ToStateEffect[A](a: A)(implicit ae: StateEffect[A]) =
    ae.stateEffect(a)
}

trait StateEffectInstances0
extends ToOperationSyntax
with ToProcessSyntax
{
  implicit def processStateEffectWrap[A: StateEffect] =
    new StateEffect[Process[Task, A]] {
      def stateEffect(prc: Process[Task, A]) =
        prc map(FlatMapEffect(_, "wrap effect").internal.success)
    }

  implicit def resultStateEffect = new StateEffect[Result]
    {
      def stateEffect(r: Result) = emit(r)
    }

  implicit def anyStateEffect[A: Operation] = new StateEffect[A]
  {
    def stateEffect(a: A) = resultStateEffect.stateEffect(a.toResult)
  }

  implicit def optionStateEffect[A: Operation] = new StateEffect[Option[A]]
  {
    def stateEffect(a: Option[A]) = {
      a some(anyStateEffect.stateEffect) none(halt)
    }
  }

  implicit def anyFunctorStateEffect[F[_]: Functor, A: Operation]
  (implicit tp: ToProcess[Task, F]) =
    new StateEffect[F[A]] {
      def stateEffect(fa: F[A]) = tp.proc(fa map(_.toResult))
    }
}

trait StateEffectInstances
extends StateEffectInstances0
{
  implicit def taskStateEffect[A: Operation] = new StateEffect[Task[A]]
  {
    def stateEffect(t: Task[A]) = Process.eval(t map(_.toResult))
  }

  implicit def processStateEffect[A: Operation] =
    new StateEffect[Process[Task, A]] {
      def stateEffect(prc: Process[Task, A]) = prc map(_.toResult)
    }

  implicit def optionalStateEffect[F[_]: Optional] =
    new StateEffect[F[Effect]] {
      def stateEffect(prc: F[Effect]) = prc getOrElse(halt)
    }

  implicit def instance_StateEffect_AnyAction[A: Operation] =
    new StateEffect[AnyAction[A]] {
      def stateEffect(fa: AnyAction[A]) = DbTask(fa).publish.success
    }

  implicit def instance_StateEffect_IO[F[_, _], A: Operation, C]
  (implicit P: PerformIO[F], cm: IOMessage[C]) =
    new StateEffect[F[A, C]] {
      def stateEffect(fa: F[A, C]) = IOTask(fa).publish.success
    }

  implicit def instance_StateEffect_IO_Unit[F[_, _], C]
  (implicit P: PerformIO[F], cm: IOMessage[C]) =
    new StateEffect[F[Unit, C]] {
      def stateEffect(fa: F[Unit, C]) = IOFork(fa, "forked IO").publish.success
    }
}

object StateEffect
extends StateEffectInstances

final class EffectOps(val self: Effect)
extends AnyVal
{
  def fork(desc: String) = Fork(self, desc).internal.success
}

trait ToEffectOps
extends Any
{
  implicit def ToEffectOps(eff: Effect) = new EffectOps(eff)
}

final class TaskEffectOps[A](val self: Task[A])
extends AnyVal
{
  def stateSideEffect(desc: String): Effect = {
    self.map(a => EffectSuccessful(desc, a).internal.success)
  }
}

final class ProcessEffectOps[A](val self: Process[Task, A])
extends AnyVal
with ToEffectOps
{
  def stateSideEffect(desc: String): Effect = {
    self map(a => EffectSuccessful(desc, a).internal.success)
  }

  def forkEffect(desc: String): Effect = {
    emit(stateSideEffect(desc).fork(desc))
  }

  def chain[O: Operation](next: Process[Task, O]) = {
    self map(_ => next: Effect)
  }
}

trait MiscEffectOps
{
  def stateEffectProc[O](description: String)
  (effect: Process[Task, O]): Effect = {
    effect map(a => EffectSuccessful(description, a).internal.success)
  }

  def stateEffectTask[O](description: String)(effect: Task[O]) = {
    stateEffectProc(description)(Process.eval(effect))
  }

  def stateEffect(description: String)(effect: => Unit) = {
    stateEffectTask(description)(Task(effect))
  }

  implicit def ToTaskEffectOps[A](t: Task[A]) = new TaskEffectOps(t)

  implicit def ToProcessEffectOps[A](t: Process[Task, A]) =
    new ProcessEffectOps(t)
}

final class TransitResultOps(val self: TransitResult)
extends AnyVal
{
  def <<[A: StateEffect](e: A): TransitResult = {
    (self._1, self._2 ++ (e: Effect))
  }

  def <<(prc: Parcel): TransitResult = {
    (self._1, self._2 ++ (prc.success: Effect))
  }
}

trait ToTransitResultOps
extends Any
{
  implicit def ToTransitResultOps(v: TransitResult) = {
    new TransitResultOps(v)
  }
}

final class ZthulhuOps(val z: Zthulhu)
extends AnyVal
with ToTransitResultOps
{
  def <<[A: StateEffect](e: A) = (z: TransitResult) << e
  def <<(prc: Parcel) = (z: TransitResult) << prc
}

trait TransitSyntax
extends ToTransitResultOps
{
  implicit def amendZthulhuWithEmptyEffects(z: Zthulhu): TransitResult =
    (z, halt)

  implicit def ToZthulhuOps(v: Zthulhu) = {
    new ZthulhuOps(v)
  }
}
