package tryp
package droid
package state

import scala.annotation.implicitNotFound

import scalaz._, Scalaz._

import shapeless._
import shapeless.tag.@@

import Process._

import droid.view._

@implicitNotFound("no Operation[${A}] defined")
trait Operation[-A]
{
  def result(a: A): Result
}

trait ParcelOperation[-A]
extends Operation[A]
{
  def result(a: A) = parcel(a).successNel[Parcel]
  def parcel(a: A): Parcel
}

final class OperationSyntax[A](a: A)(implicit op: Operation[A])
{
  def toResult = op.result(a)
}

final class ParcelOperationSyntax[A](a: A)(implicit op: ParcelOperation[A])
{
  def toParcel = op.parcel(a)
}

trait ToOperationSyntax
{
  implicit def ToOperationSyntax[A: Operation](a: A) = new OperationSyntax(a)
  implicit def ToParcelOperationSyntax[A: ParcelOperation](a: A) =
    new ParcelOperationSyntax(a)
}

trait OperationInstances
extends ToOperationSyntax
{
  implicit lazy val parcelOperation = new ParcelOperation[Parcel] {
    def parcel(prc: Parcel) = prc
  }

  implicit def messageOperation[A <: Message](implicit pf: PublishFilter[A]) =
    new ParcelOperation[A] {
      def parcel(m: A) = m
    }

  implicit def unitOperation = new ParcelOperation[Unit] {
    def parcel(u: Unit) = Parcel(UnitTask, false)
  }

  implicit def optionOperation[A: ParcelOperation]
  (implicit pf: PublishFilter[LogVerbose]) = new ParcelOperation[Option[A]] {
    def parcel(oa: Option[A]) = {
      oa
        .some(_.toParcel)
        .none(LogVerbose("empty option produced by app effect").toParcel)
    }
  }

  implicit def validationNelOperation[A: ParcelOperation] =
    new Operation[ValidationNel[String, A]] {
      def result(v: ValidationNel[String, A]): Result = {
        v.bimap(_.map(e ⇒ LogError("from nel", e).publish), _.toParcel)
      }
    }

  implicit def resultOperation = new Operation[Result] {
    def result(r: Result) = r
  }

  implicit def uiOperation[A: Operation] = new ParcelOperation[Ui[A]] {
      def parcel(u: Ui[A]) = {
        UiTask(u map(_.toResult)).publish
      }
    }

  implicit def unitUiOperation = new ParcelOperation[Ui[Unit]] {
    def parcel(u: Ui[Unit]) = {
      u map(_ ⇒ UiSuccessful("unit Ui").internal.success) toParcel
    }
  }

  implicit def anyUiOperation[A] = new ParcelOperation[Ui[A]] {
    def parcel(u: Ui[A]) = {
        u
          .map(_.toString)
          .map(UiSuccessful(_).internal)
          .toParcel
    }
  }

  implicit def iovOperation[A] = new ParcelOperation[IOV[A]] {
    def parcel(v: IOV[A]) = {
      IOVTask(v).publish
    }
  }

  implicit def tryOperation[A: Operation] = new Operation[Try[A]] {
    def result(t: Try[A]) = {
      t match {
        case Success(a) ⇒ a.toResult
        case Failure(e) ⇒ LogFatal("evaluating try", e).publish.fail
      }
    }
  }

  implicit def disjunctionOperation[A: ParcelOperation, B: ParcelOperation] =
    new Operation[\/[A, B]] {
      def result(a: \/[A, B]) = {
        a match {
          case -\/(e) ⇒ e.toParcel.fail
          case \/-(r) ⇒ r.toResult
        }
      }
    }
}

object Operation
extends OperationInstances

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

trait StateEffect[A]
{
  def proc(a: A): Effect
}

trait ToStateEffectSyntax
{
  implicit def ToStateEffect[A](a: A)(implicit ae: StateEffect[A]) =
    ae.proc(a)
}

trait StateEffectInstances0
extends ToOperationSyntax
with ToProcessSyntax
{
  implicit def resultStateEffect = new StateEffect[Result]
    {
      def proc(r: Result) = emit(r)
    }

  implicit def anyStateEffect[A: Operation] = new StateEffect[A]
  {
    def proc(a: A) = resultStateEffect.proc(a.toResult)
  }

  implicit def optionStateEffect[A: Operation] = new StateEffect[Option[A]]
  {
    def proc(a: Option[A]) = a some(anyStateEffect.proc) none(halt)
  }

  implicit def anyFunctorStateEffect[F[_]: Functor, A: Operation]
  (implicit tp: ToProcess[Task, F]) =
    new StateEffect[F[A]] {
      def proc(fa: F[A]) = tp.proc(fa map(_.toResult))
    }
}

trait StateEffectInstances
extends StateEffectInstances0
{
  implicit def taskStateEffect[A: Operation] = new StateEffect[Task[A]]
  {
    def proc(t: Task[A]) = Process.eval(t map(_.toResult))
  }

  implicit def snailStateEffect[A](ui: Ui[ScalaFuture[A]]) =
    new StateEffect[Ui[ScalaFuture[A]]] {
      def proc(u: Ui[ScalaFuture[A]]) = {
        Process.eval(Task {
          ui.run
          ForkedResult("Snail").internal.success
        })
      }
    }

  implicit def processStateEffect[A: Operation] =
    new StateEffect[Process[Task, A]] {
      def proc(prc: Process[Task, A]) = prc map(_.toResult)
    }

  implicit def optionalStateEffect[F[_]: Optional] =
    new StateEffect[F[Effect]] {
      def proc(prc: F[Effect]) = prc getOrElse(halt)
    }
}

object StateEffect
extends StateEffectInstances

final class TaskEffectOps[A](val self: Task[A])
extends AnyVal
{
  def effect(desc: String): Effect = {
    self map(a ⇒ EffectSuccessful(desc, a).internal.success)
  }
}

trait MiscEffectOps
{
  implicit lazy val uiMonad = new Monad[Ui]
  {
    def point[A](a: ⇒ A): Ui[A] = Ui(a)

    def bind[A, B](fa: Ui[A])(f: A ⇒ Ui[B]): Ui[B] =
      fa flatMap f
  }

  def stateEffectProc[O](description: String)
  (effect: Process[Task, O]): Effect = {
    effect map(a ⇒ EffectSuccessful(description, a).publish.success)
  }

  def stateEffectTask[O](description: String)(effect: Task[O]) = {
    stateEffectProc(description)(Process.eval(effect))
  }

  def stateEffect(description: String)(effect: ⇒ Unit) = {
    stateEffectTask(description)(Task(effect))
  }

  implicit def ToTaskEffectOps[A](t: Task[A]) = new TaskEffectOps(t)
}
