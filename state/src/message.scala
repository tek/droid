package tryp
package droid
package state

import core._
import view.core._
import view._

import simulacrum._

import export._

trait IOMessage[C]
{
  def pure[A: Operation](f: C => A): Message
}

object IOMessage
{
  implicit def instance_IOMessage_Context =
    new IOMessage[Context] {
      def pure[A: Operation](f: Context => A) = ContextFun(f)
    }

  implicit def instance_IOMessage_Activity =
    new IOMessage[Activity] {
      def pure[A: Operation](f: Activity => A) = ActivityFun(f)
    }

  implicit def instance_IOMessage_Resources =
    new IOMessage[Resources] {
      def pure[A: Operation](f: Resources => A) = ResourcesFun(f)
    }
}

trait IOFun
extends Message
{
  // def task
}

case class ActivityTask[A: Operation](f: Activity => A)
extends IOFun
{
  def task(implicit act: Activity) = {
    Task(f(act)) map(_.toResult)
  }
}

case class ActivityFun[A: Operation](f: Activity => A)
extends IOFun
{
  def task(implicit act: Activity) = {
    Task(f(act)) map(_.toResult)
  }
}

case class ContextTask[A: Operation](f: Context => Task[A])
extends IOFun
{
  def task_(implicit ctx: Context) = {
    f(ctx)
  }

  def task(implicit ctx: Context) = {
    task_.map(_.toResult)
  }
}

case class ContextFun[A: Operation](f: Context => A)
extends IOFun
{
  def task(implicit ctx: Context) = {
    Task(f(ctx).toResult)
  }
}

case class ContextStream[A: Operation](s: Process[Task, Context => A])
extends IOFun
{
  def task(implicit ctx: Context) = {
    s map(_(ctx).toResult)
  }
}

case class ResourcesFun[A: Operation](f: Resources => A)
extends IOFun
{
  def task(implicit res: Resources) = {
    Task(f(res)) map(_.toResult)
  }
}

case class DbTask[A: Operation](action: AnyAction[A])
extends Message

case class IOTask[F[_, _]: PerformIO, A: Operation, C](io: F[A, C])
(implicit cm: IOMessage[C])
extends Message
{
  def effect = {
    cm.pure {
      implicit c =>
        FlatMapEffect(io.unsafePerformIO, s"IOTask($io)").internal.success
    } .publish
  }
}

// TODO maybe allow A to have an Operation and asynchronously enqueue the
// results in Machine.asyncTask
case class IOMainTask[F[_, _]: PerformIO, A: Operation, C]
(io: F[A, C], timeout: Duration = Duration.Inf)
(implicit cm: IOMessage[C])
extends Message
{
  def effect = cm.pure {
    implicit c =>
      FlatMapEffect(io.main(timeout), s"IOMainTask($io)").internal.success
  } .publish
}

case class IOFork[F[_, _]: PerformIO, C](io: F[Unit, C], desc: String)
(implicit cm: IOMessage[C])
extends Message
{
  def effect = cm.pure {
    implicit c =>
      Fork(io.unsafePerformIO, desc).publish.success
  }
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
case class ViewStreamTask[A: Operation, C: IOMessage](
  stream: ViewStream[A, C], timeout: Duration = 30 seconds,
  main: Boolean = false)
extends Message
{
  private[this] val taskCtor: IO[A, C] => Message =
    if(main) IOMainTask(_) else IOTask(_)

  def effect: Effect = {
    val eff = stream.view.take(1).map(taskCtor(_).publish.success)
    FlatMapEffect(eff, toString).internal.success
  }

  override def toString = "ViewStreamTask"
}

@exports
object ViewStreamOperation
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
