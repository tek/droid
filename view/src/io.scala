package tryp
package droid
package view

import reflect.macros.blackbox

import android.support.v7.app.AppCompatActivity

import view.core._

trait AnnotatedIO
{
  def con[A](f: Context => A): IO[A, Context] =
    macro AnnotatedIOM.inst[A, Context]
  def act[A](f: Activity => A): IO[A, Activity] =
    macro AnnotatedIOM.inst[A, Activity]
  def acact[A](f: AppCompatActivity => A): IO[A, AppCompatActivity] =
    macro AnnotatedIOM.inst[A, AppCompatActivity]
  def res[A](f: Resources => A): IO[A, Resources] =
    macro AnnotatedIOM.inst[A, Resources]

  implicit def viewToIOX[A <: View](a: A): IO[A, Context] =
    macro AnnotatedIOM.now[A, Context]

  implicit def viewToApplyKestrelX[A <: View](a: A)
  : ApplyKestrel.Ops[IO, A, Context] =
    macro AnnotatedIOM.nowAK[A, Context]
}

class AnnotatedIOM(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  private[this] def withRepr
  [A: WeakTypeTag, C: WeakTypeTag, F[_, _]]
  (f: Expr[C => A], repr: String)
  (implicit fType: WeakTypeTag[F[_, _]])
  : Expr[F[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val ctor = weakTypeOf[F[_, _]].typeSymbol
    Expr {
      q"""
      new $ctor[$aType, $cType]($f, $repr)
      """
    }
  }

  def inst[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A]): Expr[IO[A, C]] =
    withRepr[A, C, IO](f, showCode(f.tree))

  def now[A: WeakTypeTag, C: WeakTypeTag](a: Expr[A]): Expr[IO[A, C]] =
    withRepr[A, C, IO](Expr(q"(_: Context) => $a"), showCode(a.tree))

  def nowAK[A: WeakTypeTag, C: WeakTypeTag](a: Expr[A])
  : Expr[ApplyKestrel.Ops[IO, A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val iType = weakTypeOf[IO[_, _]]
    val io = now[A, C](a)
    val akt = typeOf[ApplyKestrel.ops.type]
    val ak = akt.termSymbol
    val tako = typeOf[ApplyKestrel.ToApplyKestrelOps]
    val toAk = tako.member(TermName("toApplyKestrelOps"))
    Expr(q"$ak.toApplyKestrelOps(${io.tree})")
  }
}
