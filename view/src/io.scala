package tryp
package droid
package view

import reflect.macros.blackbox

import scalaz.concurrent.Task

import view.core._

case class IOX[A, C](run: C => A, desc: String)
extends IOI[A, C]
{
  def apply(implicit c: C): A = run(c)
}

trait IOXInstances
{
  implicit def instance_Monad_IOX[C]: Monad[IOX[?, C]] = new Monad[IOX[?, C]]
  {
    def pure[A](a: A) = IOX(c => a, a.toString)

    def flatMap[A, B](fa: IOX[A, C])(f: A => IOX[B, C]) = {
      IOX(c => f(fa.run(c)).run(c), s"${fa.desc}.flatMap($f)")
    }

    def tailRecM[A, B](a: A)(f: A => IOX[Either[A, B], C])
    : IOX[B, C] =
      defaultTailRecM(a)(f)
  }

  implicit lazy val instance_ConsIO_IOX = new ConsIO[IOX] {
    def cons[A, C](fa: IOX[A, C])(c: C): A = fa(c)
    def pure[A, C](run: C => A): IOX[A, C] = IOX(run, run.toString)
  }

  implicit lazy val instance_PerformIO_IOX =
    new PerformIO[IOX] {
      def unsafePerformIO[A, C](fa: IOX[A, C])(implicit c: C) = Task(fa(c))

      def main[A, C](fa: IOX[A, C])(timeout: Duration = Duration.Inf)
      (implicit c: C) = {
        PerformIO.mainTask(fa(c), timeout)
      }
    }
}

object IOX
extends IOXInstances

trait AnnotatedIO
{
  def con[A](f: Context => A): IOX[A, Context] =
    macro AnnotatedIOM.inst[A, Context]
  def act[A](f: Activity => A): IOX[A, Activity] =
    macro AnnotatedIOM.inst[A, Activity]
  def res[A](f: Resources => A): IOX[A, Resources] =
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
  : Expr[IOX[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val content = f.tree.toString
    val iType = typeOf[IOX[_, _]].typeSymbol
    Expr {
      q"""
      new $iType[$aType, $cType]($f, $content)
      """
    }
  }

  def instwrap[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A])
  : Expr[IO[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val content = f.tree.toString
    val iType = typeOf[IO[_, _]].typeSymbol
    Expr {
      q"""
      new $iType[$aType, $cType]($f)
      """
    }
  }

  def instS[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A])
  : Expr[StreamIO[A, C]] = {
    val io = instwrap[A, C](f)
    val comp = typeOf[StreamIO[_, _]].typeSymbol.companion
    Expr[StreamIO[A, C]] {
      q"""
      $comp($io)
      """
    }
  }
}

class IOXwrap[A, C](run: C => A, desc: String)
extends IO(run)
{
  override def toString = s"IO($desc)"
}

trait AnnotatedIOwrap
{
  def con[A](f: Context => A): IO[A, Context] =
    macro AnnotatedIOMwrap.inst[A, Context]
  def act[A](f: Activity => A): IO[A, Activity] =
    macro AnnotatedIOMwrap.inst[A, Activity]
  def res[A](f: Resources => A): IO[A, Resources] =
    macro AnnotatedIOMwrap.inst[A, Resources]

  def conS[A](f: Context => A): StreamIO[A, Context] =
    macro AnnotatedIOMwrap.instS[A, Context]
  def actS[A](f: Activity => A): StreamIO[A, Activity] =
    macro AnnotatedIOMwrap.instS[A, Activity]
  def resS[A](f: Resources => A): StreamIO[A, Resources] =
    macro AnnotatedIOMwrap.instS[A, Resources]
}

class AnnotatedIOMwrap(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  def inst[A: WeakTypeTag, C: WeakTypeTag](f: Expr[C => A])
  : Expr[IO[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val content = f.tree.toString
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
