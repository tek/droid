package tryp
package droid
package view

import reflect.macros.blackbox

import view.core._

class IOX[A, C](run: C => A, desc: String)
extends IO(run)

trait AnnotatedIO
{
  def con[A](f: Context => A): IO[A, Context] = 
    macro AnnotatedIOM.inst[A, Context]
  def act[A](f: Activity => A): IO[A, Activity] = 
    macro AnnotatedIOM.inst[A, Activity]
  def res[A](f: Resources => A): IO[A, Resources] = 
    macro AnnotatedIOM.inst[A, Resources]

  def conS[A](f: Context => A): StreamIO[A, Context] = 
    macro AnnotatedIOM.instS[A, Context]
  def actS[A](f: Activity => A): StreamIO[A, Activity] = 
    macro AnnotatedIOM.instS[A, Activity]
  def resS[A](f: Resources => A): StreamIO[A, Resources] = 
    macro AnnotatedIOM.instS[A, Resources]
}

class AnnotatedIOM(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  def inst[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A])
  : Expr[IO[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val content = f.toString
    val iType = typeOf[IOX[_, _]].typeSymbol
    Expr {
      q"""
      new $iType[$aType, $cType]($f, $content)
      """
    }
  }

  def instS[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A])
  : Expr[StreamIO[A, C]] = {
    val io = inst[A, C](f)
    val comp = typeOf[StreamIO[_, _]].typeSymbol.companion
    Expr[StreamIO[A, C]] {
      q"""
      $comp($io)
      """
    }
  }
}
