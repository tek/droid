package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity

import scala.concurrent.Await

import simulacrum._

import export._

import tryp.state.Fork

trait IOMessage[C]
{
  def pure[A: StateEffect](f: C => A, desc: String): Message
}

object IOMessage
{
  implicit def instance_IOMessage_Context =
    new IOMessage[Context] {
      def pure[A: StateEffect](f: Context => A, desc: String) =
        ContextFun(f, desc)
    }

  implicit def instance_IOMessage_Activity =
    new IOMessage[Activity] {
      def pure[A: StateEffect](f: Activity => A, desc: String) =
        ActivityFun(f, desc)
    }

  implicit def instance_IOMessage_AppCompatActivity =
    new IOMessage[AppCompatActivity] {
      def pure[A: StateEffect](f: AppCompatActivity => A, desc: String) =
        AppCompatActivityFun(f, desc)
    }
}

abstract class IOFun[A: StateEffect, C]
extends Message
{
  def desc: String

  def run: C => A

  def task(c: C) = Task.delay(run(c)).map(_.stateEffect)

  override def toString = s"${this.className}($desc)"
}

case class ContextFun[A: StateEffect](run: Context => A, desc: String)
extends IOFun[A, Context]

case class ActivityFun[A: StateEffect](run: Activity => A, desc: String)
extends IOFun[A, Activity]

case class AppCompatActivityFun[A: StateEffect]
(run: AppCompatActivity => A, desc: String)
extends IOFun[A, AppCompatActivity]

@typeclass trait FromContext[C]
{
  def summon(c: Context): C
}

object FromContext
{
  implicit def instance_FromContext_Resources: FromContext[Resources] =
    new FromContext[Resources] {
      def summon(c: Context) = Resources.fromContext(c)
    }
}

trait InternalIOMessage
extends Message
{
  def effect(implicit sender: Sender): Effect
}

case class FromContextIO[F[_, _]: PerformIO, A: StateEffect, C: FromContext]
(io: F[A, C])(implicit cv: Contravariant[F[A, ?]])
extends InternalIOMessage
{
  def effect(implicit sender: Sender) = {
    IOTask(io contramap FromContext[C].summon, io.toString).effect
  }
}

case class ECTask[A: StateEffect](run: EC => A)
extends Message
{
  def effect = (ec: EC) => (run(ec).stateEffect)
}

case class IOTask[F[_, _]: PerformIO, A: StateEffect, C]
(io: F[A, C], desc: String)
(implicit cm: IOMessage[C])
extends Message
{
  val task = (c: C) => io.unsafePerformIO(c)

  def effect(implicit sender: Sender) =
    cm.pure(task, desc).broadcast.stateEffect

  override def toString = s"IOTask($desc)"
}

// TODO maybe allow A to have an Operation and asynchronously enqueue the
// results in Machine.asyncTask
case class IOMainTask[F[_, _]: PerformIO, A, C]
(io: F[A, C], desc: String, timeout: Duration = 3.second)
(implicit cm: IOMessage[C], se: StateEffect[Task[A]])
extends Message
{
  def effect(implicit sched: Scheduler, sender: Sender) =
    cm.pure(io.mainTimed(timeout)(_, sched), desc).broadcast

  override def toString = s"IOMainTask($desc)"
}

case class IOFork[F[_, _]: PerformIO, C](io: F[Unit, C], desc: String)
(implicit im: IOMessage[C])
extends Message
{
  def effect(implicit sender: Sender) = im.pure (
    implicit c => Fork(io.unsafePerformIO.stateEffect, desc).publish,
    desc
  ).stateEffect

  override def toString = s"IOFork($desc)"
}

final class IOEffect[F[_, _]: PerformIO]
extends AnyRef
{
  def ui[A: Operation, C](fa: F[A, C])
  (implicit cm: IOMessage[C], sender: Sender) =
    IOMainTask(fa, fa.toString).broadcast
}

object IOEffect
{
  implicit def instance_IOEffect_PerformIO[F[_, _]: PerformIO]: IOEffect[F] =
    new IOEffect[F]

  abstract class Ops[F[_, _]: PerformIO, A, C: IOMessage]
  (implicit sender: Sender)
  {
    def typeClassInstance: IOEffect[F]
    def self: F[A, C]

    def ui(implicit op: Operation[A]) =
      typeClassInstance.ui[A, C](self)

    def unitUi(implicit M: Monad[F[?, C]]) =
      typeClassInstance.ui[Unit, C](self map (_ => ()))
  }

  trait ToIOEffectOps
  {
    implicit def toIOEffectOps[F[_, _]: PerformIO, A, C: IOMessage]
    (fa: F[A, C])
    (implicit tc: IOEffect[F], sender: Sender) =
      new Ops[F, A, C] {
        val self = fa
        val typeClassInstance = tc
      }
  }

  object ops
  extends ToIOEffectOps
}

@exports
object IOOperation
{
  @export(Instantiated)
  implicit def instance_Operation_IO
  [F[_, _]: PerformIO, A: Operation, C: IOMessage]: Operation[F[A, C]]
  = Operation.message((a: F[A, C]) => IOTask(a, a.toString))

  @export(Instantiated)
  implicit def instance_Operation_UnitIO
  [F[_, _]: PerformIO, C: IOMessage]
  = Operation.message(IOFork((_: F[Unit, C]), "forked IO"))

  /* fork IOs that return View
   */
  @export(Instantiated)
  implicit def instance_StateEffect_IO_View
  [A <: View, F[_, _]: PerformIO, C: IOMessage]
  (implicit fu: cats.Functor[F[?, C]]): StateEffect[F[A, C]] =
    new StateEffect[F[A, C]] {
      def stateEffect(io: F[A, C]) = {
        IOTask(io.void, io.toString).publish.stateEffect
      }
    }
}
