package tryp
package droid
package state
package core

import reflect.macros.blackbox

import android.support.v7.app.AppCompatActivity

import view.core._

trait IORunner[A]
extends Message
{
  def io: IO[Message, A]

  def desc = io.desc

  def runOnMain: Boolean = false

  def task(a: A)(implicit sched: Scheduler): Task[Message] =
    if (runOnMain) io.mainTimed(3.seconds)(a, sched)
    else io.unsafePerformIO(a)
}

case class ContextIO(io: IO[Message, Context])
extends IORunner[Context]
{
  def main = new ContextIO(io) { override def runOnMain = true }
}

case class ActivityIO(io: IO[Message, Activity])
extends IORunner[Activity]
{
  def main = new ActivityIO(io) { override def runOnMain = true }
}

case class AppCompatActivityIO(io: IO[Message, AppCompatActivity])
extends IORunner[AppCompatActivity]
{
  def main = new AppCompatActivityIO(io) { override def runOnMain = true }
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
