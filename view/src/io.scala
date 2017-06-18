package tryp
package droid
package view

import reflect.macros.blackbox

import android.support.v7.app.AppCompatActivity

import view.core._

trait AnnotatedAIO
{
  def conIO[A](f: Context => A): AIO[A, Context] =
    macro AnnotatedAIOM.inst[A, Context]
  def actIO[A](f: Activity => A): AIO[A, Activity] =
    macro AnnotatedAIOM.inst[A, Activity]
  def acactIO[A](f: AppCompatActivity => A): AIO[A, AppCompatActivity] =
    macro AnnotatedAIOM.inst[A, AppCompatActivity]
  def resIO[A](f: Resources => A): AIO[A, Resources] =
    macro AnnotatedAIOM.inst[A, Resources]
}

trait ViewToAIO
{
  implicit def viewToAIO[A <: View](a: A): AIO[A, Context] =
    macro AnnotatedAIOM.now[A, Context]

  implicit def viewToApplyKestrel[A <: View](a: A)
  : ApplyKestrel.Ops[AIO, A, Context] =
    macro AnnotatedAIOM.nowAK[A, Context]
}

class AnnotatedAIOM(val c: blackbox.Context)
extends MacroMetadata
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

  def inst[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A]): Expr[AIO[A, C]] =
    withRepr[A, C, AIO](f, showCode(f.tree))

  def now[A: WeakTypeTag, C: WeakTypeTag](a: Expr[A]): Expr[AIO[A, C]] =
    withRepr[A, C, AIO](Expr(q"(_: Context) => $a"), showCode(a.tree))

  def nowAK[A: WeakTypeTag, C: WeakTypeTag](a: Expr[A])
  : Expr[ApplyKestrel.Ops[AIO, A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val iType = weakTypeOf[AIO[_, _]]
    val io = now[A, C](a)
    val akt = typeOf[ApplyKestrel.ops.type]
    val ak = akt.termSymbol
    val tako = typeOf[ApplyKestrel.ToApplyKestrelOps]
    val toAk = tako.member(TermName("toApplyKestrelOps"))
    Expr(q"$ak.toApplyKestrelOps(${io.tree})")
  }
}
