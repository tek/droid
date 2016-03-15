package tryp
package droid
package view

import concurrent.Await

import reflect.macros._

import simulacrum._

import shapeless._
import shapeless.ops.hlist._

import scalaz.concurrent.Task

import cats._

@typeclass trait OI[F[_]]
{
  def unsafePerformIO[A](fa: F[A])(): A
  def main[A](fa: F[A]): Task[A]
}

object OI
{
  implicit val ioOi = new OI[iota.IO] {
    def unsafePerformIO[A](fa: iota.IO[A])() = {
      fa.perform()
    }

    // TODO use Task.fork and supply ES with android handler manually
    def main[A](fa: iota.IO[A]) = {
      Task {
        Await.result(fa.performMain(), Duration.Inf)
      }
    }
  }
}

// @typeclass abstract class CIO[F[_]: OI]
// {
//   def map[A, B](fa: F[A])(f: Context => A => B): F[B]
// }

@typeclass trait OICtor[A]
{

}

case class OIA[A: ClassTag, B](ctor: B => A)

trait LOIABase[A, B]
{
  def l: OIA[A, B]
}

case class LOIA[A, B, C <: HList](l: OIA[A, B], sub: C)
extends LOIABase[A, B]

trait OIViews[C]
{
  def w[A]: OIA[A, C] = macro OIViewM.w[A, C]

  def l[A](inner: Any*): LOIABase[A, C] = macro OIViewM.l[A, C]
}

class OIViewM(val c: whitebox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  def w[A: WeakTypeTag, C: WeakTypeTag]: Expr[OIA[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    Expr[OIA[A, C]] {
      q"""
      tryp.droid.view.OIA((ctx: $cType) => new $aType(ctx))
      """
    }
  }

  def l[A: c.WeakTypeTag, C: WeakTypeTag]
  (inner: Expr[Any]*): Expr[LOIABase[A, C]] = {
    val aType = weakTypeOf[A]
    val sub = inner.foldLeft(q"shapeless.HNil": Tree) {
      case (z, v) => q"$v :: $z"
    }
    Expr[LOIABase[A, C]] {
      q"""
        tryp.droid.view.LOIA(w[$aType], $sub)
      """
    }
  }
}

case class ConIO[A](f: Context => A)

case class ActIO[A](f: Activity => A)
