package tryp
package droid
package state

import org.log4s.Logger

import scalaz._, Scalaz._

import Process._

trait ToMessage[A]
{
  def message(a: A): Message
  def error(a: A): Message
}

trait ToMessageInstances0
{
  implicit def anyToMessage[A] = new ToMessage[A] {
    def message(a: A) = {
      UnknownResult(a.toString)
    }
    def error(a: A) = LogError("running unknown effect", a.toString)
  }
}

trait ToMessageInstances
extends ToMessageInstances0
{
  implicit def showableToMessage[A: Show] = new ToMessage[A] {
    def message(a: A) = UnknownResult(a)
    def error(a: A) = LogError("running unknown task", a.toString)
  }
}

object ToMessage
extends ToMessageInstances
{
  implicit def messageToMessage[A <: Message] = new ToMessage[A] {
    def message(a: A) = a
    def error(a: A) = a
  }

  implicit def futureToMessage[A] = new ToMessage[ScalaFuture[A]] {
    private[this] def fail = sys.error("Tried to convert future to message")
    def message(f: Future[A]) = fail
    def error(f: Future[A]) = fail
  }
}

object ToMessageSyntax
{
  implicit class ToMessageOps[A](a: A)(implicit tm: ToMessage[A]) {
    def toErrorMessage = tm.error(a)
    def toMessage = tm.message(a)
  }
}
import ToMessageSyntax._

trait Operation[-A]
{
  def result(a: A): Result
}

final class OperationSyntax[A](a: A)(implicit op: Operation[A])
{
  def toResult = op.result(a)
}

trait ToOperationSyntax
{
  implicit def ToOperationSyntax[A: Operation](a: A) = new OperationSyntax(a)
}

trait OperationInstances0
extends ToOperationSyntax
{
  implicit def messageOperation = new Operation[Message] {
    def result(m: Message) = m.successNel[Message]
  }

  implicit def resultUiOperation = new Operation[Ui[Result]] {
    def result(u: Ui[Result]) = {
      ToOperationSyntax(UiTask(u))(messageOperation).toResult
    }
  }

  implicit def unitUiOperation = new Operation[Ui[Unit]] {
    def result(u: Ui[Unit]) = {
      u map(_ ⇒ UiSuccessful("unit Ui").toResult) toResult
    }
  }

  implicit def anyUiOperation = new Operation[Ui[Any]] {
    def result(u: Ui[Any]) = {
      u map(_.toString) map(UiSuccessful(_).toResult) toResult
    }
  }

  implicit def optionOperation[A: Operation] = new Operation[Option[A]] {
    def result(oa: Option[A]) = {
      oa
        .some(_.toResult)
        .none(LogVerbose("empty option produced by app effect").toResult)
    }
  }
}

trait OperationInstances
extends OperationInstances0
{
  implicit def validationNelOperation[E: ToMessage, A: ToMessage] =
    new Operation[ValidationNel[E, A]] {
      def result(v: ValidationNel[E, A]): Result = {
        v.bimap(_.map(e ⇒ e.toErrorMessage), _.toMessage)
      }
    }

  implicit def uiOperation[A: Operation] = new Operation[Ui[A]] {
    def result(u: Ui[A]) = u map(_.toResult) toResult
  }

  implicit def tryOperation[A: Operation] = new Operation[Try[A]] {
    def result(t: Try[A]) = {
      t match {
        case Success(a) ⇒ a.toResult
        case Failure(e) ⇒ LogFatal("evaluating try", e).toResult
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
  def proc(a: A): Process[Task, Result]
}

trait ToStateEffectSyntax
{
  implicit def ToStateEffect[A](a: A)(implicit ae: StateEffect[A]) = ae.proc(a)
}

trait StateEffectInstances0
extends ToOperationSyntax
with ToProcessSyntax
{
  implicit lazy val resultStateEffect = new StateEffect[Result]
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

  implicit def snailStateEffect[A](ui: Ui[ScalaFuture[A]])
  (implicit ec: EC) =
    Task {
      ui.run
      ForkedResult("Snail").toResult
    }

}

trait StateEffectInstances
extends StateEffectInstances0
{
  implicit def taskStateEffect[A: Operation] = new StateEffect[Task[A]]
  {
    def proc(t: Task[A]) = Process.eval(t map(_.toResult))
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

final class TaskEffectOps[A](t: Task[A])
{
  def effect(desc: String) = {
    t map(_ ⇒ EffectSuccessful(desc).toResult)
  }
}

final class ProcessEffectOps[F[_], A](proc: Process[F, A])
{
  def logged(desc: String)(implicit logger: Logger) = {
    proc |> stream.process1.lift { m ⇒
      logger.trace(s"$desc delivering $m")
      m
    }
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

  def stateEffectProc[F[_], O](description: String)(effect: Process[F, O]) = {
    effect map(EffectSuccessful(description, _).toResult)
  }

  def stateEffectTask[F[_], O](description: String)(effect: F[O]) = {
    stateEffectProc(description)(Process.eval(effect))
  }

  def stateEffect(description: String)(effect: ⇒ Unit) = {
    stateEffectTask(description)(Task(effect))
  }

  implicit def ToProcessEffectOps[F[_], A](proc: Process[F, A]) =
    new ProcessEffectOps(proc)

  implicit def ToTaskEffectOps[A](proc: Task[A]) = new TaskEffectOps(proc)
}

trait Exports
extends MiscEffectOps
with ToOperationSyntax
with ToProcessSyntax
with ToStateEffectSyntax
{
  val Nop: Effect = Process.halt

  def signalSetter[A] = stream.process1.lift[A, Signal.Set[A]](Signal.Set(_))

  type Agent = droid.state.Agent
  type SolitaryAgent = droid.state.SolitaryAgent
  type Mediator = droid.state.Mediator
  type HasContextAgent = droid.state.HasContextAgent
  type HasActivityAgent = droid.state.HasActivityAgent
  type ActivityAgent = droid.state.ActivityAgent
  type FragmentAgent = droid.state.FragmentAgent
}
