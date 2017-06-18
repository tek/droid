package tryp
package droid
package state
package core

import reflect.macros.blackbox

import android.support.v7.app.AppCompatActivity

import view.core._

trait AIORunner[A, B]
extends Message
{
  def io: AIO[Message, A]

  def desc = io.desc

  def runOnMain: Boolean = false

  def main: B

  def task(a: A): IO[Message] =
    if (runOnMain) io.performMain(a)
    else io.perform(a)
}

case class ContextAIO(io: AIO[Message, Context])
extends AIORunner[Context, ContextAIO]
{
  def main = new ContextAIO(io) { override def runOnMain = true }
}

case class ActivityAIO(io: AIO[Message, Activity])
extends AIORunner[Activity, ActivityAIO]
{
  def main = new ActivityAIO(io) { override def runOnMain = true }
}

case class AppCompatActivityAIO(io: AIO[Message, AppCompatActivity])
extends AIORunner[AppCompatActivity, AppCompatActivityAIO]
{
  def main = new AppCompatActivityAIO(io) { override def runOnMain = true }
}

trait ToAIORunner[C]
{
  type R <: AIORunner[C, R]

  def pure(io: AIO[Message, C]): R
}

object ToAIORunner
{
  implicit def ToAIORunner_Context: ToAIORunner[Context] =
    new ToAIORunner[Context] {
      type R = ContextAIO
      def pure(io: AIO[Message, Context]) = ContextAIO(io)
    }
}

final class AIOStateOps[A <: Message, C](val io: AIO[A, C])
{
  def runner(implicit toRunner: ToAIORunner[C]) = toRunner.pure(io.widen[Message])

  def main(implicit toRunner: ToAIORunner[C]) = runner.main
}

final class AIOStateAnyOps[A, C](val io: AIO[A, C])
{
  def unit(implicit toRunner: ToAIORunner[C]) = toRunner.pure(io.map(_ => NopMessage))

  def unitMain(implicit toRunner: ToAIORunner[C]) = unit.main
}

trait ToAIOStateOps
{
  implicit def ToAIOStateOps[A <: Message, C](io: AIO[A, C]): AIOStateOps[A, C] =
    new AIOStateOps[A, C](io)

  implicit def ToAIOStateAnyOps[A, C](io: AIO[A, C]): AIOStateAnyOps[A, C] =
    new AIOStateAnyOps[A, C](io)
}

trait AnnotatedTAIO
{
  def con[A](f: Context => A): ContextAIO =
    macro AnnotatedTAIOM.inst[A, Context, ContextAIO]

  def act[A](f: Activity => A): ActivityAIO =
    macro AnnotatedTAIOM.inst[A, Activity, ActivityAIO]

  def acact[A](f: AppCompatActivity => A): AppCompatActivityAIO =
    macro AnnotatedTAIOM.inst[A, AppCompatActivity, AppCompatActivityAIO]

  def conU(f: Context => Unit): ContextAIO =
    macro AnnotatedTAIOM.instU[Context, ContextAIO]

  def actU(f: Activity => Unit): ActivityAIO =
    macro AnnotatedTAIOM.instU[Activity, ActivityAIO]

  def acactU(f: AppCompatActivity => Unit): AppCompatActivityAIO =
    macro AnnotatedTAIOM.instU[AppCompatActivity, AppCompatActivityAIO]

  def inflate[A]: AIO[A, Context] =
    macro AnnotatedTAIOM.inflate[A]
}

class AnnotatedTAIOM(val c: blackbox.Context)
extends MacroMetadata
{
  import c.universe._
  import c.Expr

  def ioWithRepr[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A]): Expr[AIO[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val repr = showCode(f.tree)
    Expr(q"AIO[$aType, $cType]($f, $repr)")
  }

  def inst[A: WeakTypeTag, C: WeakTypeTag, AIO: WeakTypeTag](f: Expr[C => A]): Expr[AIO] = {
    val aType = weakTypeOf[A]
    val ctor = weakTypeOf[AIO].typeSymbol
    val io = ioWithRepr[A, C](f)
    Expr(q"new $ctor($io.map(a => implicitly[Parcel[$aType]].msg(a): tryp.state.core.Message))")
  }

  def instU[C: WeakTypeTag, AIO: WeakTypeTag](f: Expr[C => Unit]): Expr[AIO] = {
    val ctor = weakTypeOf[AIO].typeSymbol
    val io = ioWithRepr[Unit, C](f)
    Expr(q"new $ctor($io.map(_ => NopMessage))")
  }

  def inflate[A: WeakTypeTag]: Expr[AIO[A, Context]] = {
    val aType = weakTypeOf[A]
    val vtree = aType.typeSymbol.companion
    ioWithRepr[A, Context](Expr[Context => A](q"(ctx: Context) => iota.ViewTree.inflate(ctx, $vtree)"))
  }
}

trait AIOParcel
{
  implicit def instance_Parcel_AIO_Message_Context[A, M <: Message]
  (implicit P: Parcel.Aux[A, M])
  : Parcel.Aux[AIO[A, Context], ContextAIO] =
    new Parcel[AIO[A, Context]] {
      type M = ContextAIO
      def msg(a: AIO[A, Context]) = ContextAIO(a.map(P.msg))
    }

  implicit def instance_Parcel_AIO_Message_Message[A <: Message]
  : Parcel.Aux[AIO[A, Context], ContextAIO] =
    new Parcel[AIO[A, Context]] {
      type M = ContextAIO
      def msg(a: AIO[A, Context]) = ContextAIO(a.widen[Message])
    }
}
