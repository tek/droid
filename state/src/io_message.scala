package tryp
package droid
package state

import reflect.macros.blackbox

import android.support.v7.app.AppCompatActivity

import scala.concurrent.Await

import simulacrum._

import export._

trait IOMessage[C]
{
  def pure[A, M <: Message](f: C => A, desc: String)(implicit P: Parcel.Aux[A, M]): Message =
    cons(f andThen (P.msg), desc)

  def cons(f: C => Message, desc: String): Message
}

object IOMessage
{
  implicit def instance_IOMessage_Context =
    new IOMessage[Context] {
      def cons(f: Context => Message, desc: String) =
        ContextFun(f, desc)
    }

  implicit def instance_IOMessage_Activity =
    new IOMessage[Activity] {
      def cons(f: Activity => Message, desc: String) =
        ActivityFun(f, desc)
    }

  implicit def instance_IOMessage_AppCompatActivity =
    new IOMessage[AppCompatActivity] {
      def cons(f: AppCompatActivity => Message, desc: String) =
        AppCompatActivityFun(f, desc)
    }
}

abstract class IOFun[C]
extends Message
{
  def desc: String

  def run: C => Message

  def task(c: C) = Task.delay(run(c))

  override def toString = s"${this.className}($desc)"
}

case class ContextFun(run: Context => Message, desc: String)
extends IOFun[Context]

case class ActivityFun(run: Activity => Message, desc: String)
extends IOFun[Activity]

case class AppCompatActivityFun
(run: AppCompatActivity => Message, desc: String)
extends IOFun[AppCompatActivity]

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
  def msg(implicit sender: Sender): Message
}

// case class FromContextIO[F[_, _]: PerformIO, A: Parcel, C: FromContext]
// (io: F[A, C])(implicit cv: Contravariant[F[A, ?]])
// extends InternalIOMessage
// {
//   def msg(implicit sender: Sender) = {
//     IOTask(io contramap FromContext[C].summon, io.toString).msg
//   }
// }

// case class ECTask[A: Parcel](run: EC => A)
// extends Message
// {
//   def msg = (ec: EC) => (run(ec).msg)
// }

trait IOTaskBase
extends Message
{
  def msg(implicit sender: Sender): Message
}

// case class IOTask[F[_, _]: PerformIO, A: Parcel, C]
// (io: F[A, C], desc: String)
// (implicit ioMessage: IOMessage[C])
// extends IOTaskBase
// {
//   val task = (c: C) => io.unsafePerformIO(c)

//   def msg(implicit sender: Sender): Message =
//     ioMessage.pure(task, desc)

//   override def toString = s"IOTask($desc)"
// }

// // TODO maybe allow A to have an Operation and asynchronously enqueue the
// // results in Machine.asyncTask
// case class IOMainTask[F[_, _]: PerformIO, A, C]
// (io: F[A, C], desc: String, timeout: Duration = 3.second)
// (implicit ioMessage: IOMessage[C], se: Parcel[Task[A]])
// extends Message
// {
//   def msg(implicit sched: Scheduler, sender: Sender) =
//     ioMessage.pure(io.mainTimed(timeout)(_, sched), desc)

//   override def toString = s"IOMainTask($desc)"
// }

// case class IOFork[F[_, _]: PerformIO, C](io: F[Unit, C], desc: String)
// (implicit im: IOMessage[C])
// extends Message
// {
//   def msg(implicit sender: Sender) = im.pure (
//     implicit c => Fork(io.unsafePerformIO.msg, desc).publish,
//     desc
//   ).msg

//   override def toString = s"IOFork($desc)"
// }

// final class IOEffect[F[_, _]: PerformIO]
// extends AnyRef
// {
//   def ui[A: Parcel, C](fa: F[A, C])(implicit ioMessage: IOMessage[C], sender: Sender) =
//     IOMainTask(fa, fa.toString)
// }

// object IOEffect
// {
//   implicit def instance_IOEffect_PerformIO[F[_, _]: PerformIO]: IOEffect[F] =
//     new IOEffect[F]

//   abstract class Ops[F[_, _]: PerformIO, A, C: IOMessage]
//   (implicit sender: Sender)
//   {
//     def typeClassInstance: IOEffect[F]
//     def self: F[A, C]

//     def ui(implicit prc: Parcel[A]) =
//       typeClassInstance.ui[A, C](self)

//     def unitUi(implicit M: Monad[F[?, C]]) =
//       typeClassInstance.ui[Unit, C](self map (_ => ()))
//   }

//   trait ToIOEffectOps
//   {
//     implicit def toIOEffectOps[F[_, _]: PerformIO, A, C: IOMessage]
//     (fa: F[A, C])
//     (implicit tc: IOEffect[F], sender: Sender) =
//       new Ops[F, A, C] {
//         val self = fa
//         val typeClassInstance = tc
//       }
//   }

//   object ops
//   extends ToIOEffectOps
// }

// @exports
// object IOOperation
// {
//   @export(Instantiated)
//   implicit def instance_Operation_IO[F[_, _]: PerformIO, A: Operation, C: IOMessage]: Operation[F[A, C]] =
//     Operation.message((a: F[A, C]) => IOTask(a, a.toString))

//   @export(Instantiated)
//   implicit def instance_Operation_UnitIO[F[_, _]: PerformIO, C: IOMessage] =
//     Operation.message(IOFork((_: F[Unit, C]), "forked IO"))

//   /* fork IOs that return View
//    */
//   @export(Instantiated)
//   implicit def instance_StateEffect_IO_View[A <: View, F[_, _]: PerformIO, C: IOMessage]
//   (implicit fu: cats.Functor[F[?, C]]): Parcel[F[A, C]] =
//     new Parcel[F[A, C]] {
//       def msg(io: F[A, C]) = IOTask(io.void, io.toString).publish.msg
//     }
// }

trait IOParcel
{
  implicit def instance_Parcel_IO_Context[A, M <: Message]
  (implicit P: Parcel.Aux[A, M])
  : Parcel.Aux[IO[A, Context], ContextIO] =
    new Parcel[IO[A, Context]] {
      type M = ContextIO
      def msg(a: IO[A, Context]) = ContextIO(a.map(P.msg))
    }
}
