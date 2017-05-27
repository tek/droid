package tryp
package droid
package state
package core

import reflect.macros.blackbox

import android.support.v7.app.AppCompatActivity

import view.core._

trait IORunner[A, B]
extends Message
{
  def io: IO[Message, A]

  def desc = io.desc

  def runOnMain: Boolean = false

  def main: B

  def task(a: A)(implicit sched: Scheduler): Task[Message] =
    if (runOnMain) io.mainTimed(3.seconds)(a, sched)
    else io.perform(a)
}

case class ContextIO(io: IO[Message, Context])
extends IORunner[Context, ContextIO]
{
  def main = new ContextIO(io) { override def runOnMain = true }
}

case class ActivityIO(io: IO[Message, Activity])
extends IORunner[Activity, ActivityIO]
{
  def main = new ActivityIO(io) { override def runOnMain = true }
}

case class AppCompatActivityIO(io: IO[Message, AppCompatActivity])
extends IORunner[AppCompatActivity, AppCompatActivityIO]
{
  def main = new AppCompatActivityIO(io) { override def runOnMain = true }
}

trait ToIORunner[C]
{
  type R <: IORunner[C, R]

  def pure(io: IO[Message, C]): R
}

object ToIORunner
{
  implicit def ToIORunner_Context: ToIORunner[Context] =
    new ToIORunner[Context] {
      type R = ContextIO
      def pure(io: IO[Message, Context]) = ContextIO(io)
    }
}

final class IOStateOps[C](val io: IO[Message, C])
{
  def runner(implicit toRunner: ToIORunner[C]) = toRunner.pure(io)

  def main(implicit toRunner: ToIORunner[C]) = runner.main
}

final class IOStateAnyOps[A, C](val io: IO[A, C])
{
  def unit(implicit toRunner: ToIORunner[C]) = toRunner.pure(io.map(_ => NopMessage))

  def unitMain(implicit toRunner: ToIORunner[C]) = unit.main
}

trait ToIOStateOps
{
  implicit def ToIOStateOps[C](io: IO[Message, C]): IOStateOps[C] =
    new IOStateOps[C](io)

  implicit def ToIOStateAnyOps[A, C](io: IO[A, C]): IOStateAnyOps[A, C] =
    new IOStateAnyOps[A, C](io)
}

trait AnnotatedTIO
{
  def con[A](f: Context => A): ContextIO =
    macro AnnotatedTIOM.inst[A, Context, ContextIO]

  def act[A](f: Activity => A): ActivityIO =
    macro AnnotatedTIOM.inst[A, Activity, ActivityIO]

  def acact[A](f: AppCompatActivity => A): AppCompatActivityIO =
    macro AnnotatedTIOM.inst[A, AppCompatActivity, AppCompatActivityIO]

  def inflate[A]: IO[A, Context] =
    macro AnnotatedTIOM.inflate[A]
}

class AnnotatedTIOM(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  def ioWithRepr[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A]): Expr[IO[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val repr = showCode(f.tree)
    Expr(q"IO[$aType, $cType]($f, $repr)")
  }

  def inst[A: WeakTypeTag, C: WeakTypeTag, IO: WeakTypeTag](f: Expr[C => A]): Expr[IO] = {
    val aType = weakTypeOf[A]
    val ctor = weakTypeOf[IO].typeSymbol
    val io = ioWithRepr[A, C](f)
    Expr(q"new $ctor($io.map(a => implicitly[Parcel[$aType]].msg(a): tryp.state.core.Message))")
  }

  def inflate[A: WeakTypeTag]: Expr[IO[A, Context]] = {
    val aType = weakTypeOf[A]
    val vtree = aType.typeSymbol.companion
    ioWithRepr[A, Context](Expr[Context => A](q"(ctx: Context) => iota.ViewTree.inflate(ctx, $vtree)"))
  }
}
