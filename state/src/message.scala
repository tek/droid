package tryp
package droid
package state

import tryp.slick._
import tryp.state._

import view.core._
import view._
import droid.core.Db

import scala.concurrent.Await

import simulacrum._

import export._

trait IOMessage[C]
{
  def pure[A: StateEffect](f: C => A): Message
}

object IOMessage
{
  implicit def instance_IOMessage_Context =
    new IOMessage[Context] {
      def pure[A: StateEffect](f: Context => A) = ContextFun(f)
    }

  implicit def instance_IOMessage_Activity =
    new IOMessage[Activity] {
      def pure[A: StateEffect](f: Activity => A) = ActivityFun(f)
    }
}

abstract class IOFun[A: StateEffect, C]
extends Message
{
  def run: C => A

  def task(c: C) = ZTask(run(c)).map(_.stateEffect)
}

case class ContextFun[A: StateEffect](run: Context => A)
extends IOFun[A, Context]

case class ActivityFun[A: StateEffect](run: Activity => A)
extends IOFun[A, Activity]

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

  implicit def instance_FromContext_DbInfo: FromContext[DbInfo] =
    new FromContext[DbInfo] {
      def summon(c: Context) = Db.fromContext(c)
    }
}

trait InternalIOMessage
extends Message
{
  def effect: Effect
}

case class FromContextIO[F[_, _]: PerformIO, A, C: FromContext]
(io: F[A, C])(implicit cv: Contravariant[F[A, ?]], se: StateEffect[ZTask[A]])
extends InternalIOMessage
{
  def effect = {
    IOTask(io contramap FromContext[C].summon).effect
  }
}

case class ECTask[A: StateEffect](run: EC => A)
extends Message
{
  def effect = (ec: EC) => (run(ec).stateEffect)
}

case class DbTask[A: Operation, E <: SlickEffect](action: SlickAction[A, E])
extends Message
{
  def task(dbi: DbInfo): Effect = {
    ZTask(Await.result(dbi.db() run(action), Duration.Inf)).stateEffect
  }

  def effect(dbi: Option[DbInfo]): Effect = {
    dbi.map(task) | {
      val io = IO((dbi: DbInfo) => task(dbi))
      val fc = FromContextIO[IO, Effect, DbInfo](io)
      fc.effect
    }
  }
}

case class IOTask[F[_, _]: PerformIO, A, C](io: F[A, C])
(implicit cm: IOMessage[C], se: StateEffect[ZTask[A]])
extends Message
{
  def effect = cm.pure(io.unsafePerformIO(_)).publish.stateEffect
}

// TODO maybe allow A to have an Operation and asynchronously enqueue the
// results in Machine.asyncTask
case class IOMainTask[F[_, _]: PerformIO, A, C]
(io: F[A, C], timeout: Duration = 1.second)
(implicit cm: IOMessage[C], se: StateEffect[ZTask[A]])
extends Message
{
  def effect = cm.pure(io.main(timeout)(_)).publish
}

case class IOFork[F[_, _]: PerformIO, C](io: F[Unit, C], desc: String)
(implicit im: IOMessage[C])
extends Message
{
  def effect = im.pure {
    implicit c =>
      Fork(io.unsafePerformIO.stateEffect, desc).publish.success
  }.stateEffect
}

final class IOEffect[F[_, _]: PerformIO]
extends AnyRef
{
  def ui[A: Operation, C](fa: F[A, C])(implicit cm: IOMessage[C]) =
    IOMainTask(fa).publish.success
}

object IOEffect
{
  implicit def instance_IOEffect_PerformIO[F[_, _]: PerformIO]: IOEffect[F] =
    new IOEffect[F]

  abstract class Ops[F[_, _]: PerformIO, A, C: IOMessage]
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
    (implicit tc: IOEffect[F]) =
      new Ops[F, A, C] {
        val self = fa
        val typeClassInstance = tc
      }
    }

  object ops
  extends ToIOEffectOps
}

// FIXME this should be handled by the same machine and thus not need
// FlatMapEffect and publish
case class ViewStreamTask[A, C: IOMessage](
  stream: ViewStream[A, C], timeout: Duration = 30 seconds,
  main: Boolean = false)
(implicit se: StateEffect[ZTask[A]])
extends Message
{
  private[this] val taskCtor: IO[A, C] => Message =
    if(main) IOMainTask(_) else IOTask(_)

  def effect: Effect = {
    val eff = stream.view.take(1).map(taskCtor(_).publish.success)
    Effect(eff, "view stream")
  }

  override def toString = "ViewStreamTask"
}

@exports
object IOOperation
{
  @export(Instantiated)
  implicit def instance_Operation_ViewStream[A: Operation, C: IOMessage]:
  Operation[ViewStream[A, C]] =
    new ParcelOperation[ViewStream[A, C]] {
      def parcel(v: ViewStream[A, C]) = {
        ViewStreamTask(v).publish
      }

      override def toString = "Operation[ViewStream]"
    }

  @export(Instantiated)
  implicit def instance_StateEffect_ViewStream[A, C: IOMessage]
  (implicit se: StateEffect[ZTask[A]])
  :
  StateEffect[ViewStream[A, C]] =
    new StateEffect[ViewStream[A, C]] {
      def stateEffect(v: ViewStream[A, C]) = {
        ViewStreamTask(v).publish.stateEffect
      }

      override def toString = "StateEffect[ViewStream]"
    }

  @export(Instantiated)
  implicit def instance_Operation_IO
  [F[_, _]: PerformIO, A: Operation, C: IOMessage]: Operation[F[A, C]]
  = Operation.message(IOTask(_: F[A, C]))

  @export(Instantiated)
  implicit def instance_Operation_UnitIO
  [F[_, _]: PerformIO, C: IOMessage]
  = Operation.message(IOFork((_: F[Unit, C]), "forked IO"))
}

final class ViewStreamMessageOps[A: Operation, C: IOMessage]
(val self: ViewStream[A, C])
{
  def main = ViewStreamTask(self, main = true).publish
}

trait ToViewStreamMessageOps
{
  implicit def ToViewStreamMessageOps[A: Operation, C: IOMessage]
  (vs: ViewStream[A, C]) =
    new ViewStreamMessageOps(vs)
}
