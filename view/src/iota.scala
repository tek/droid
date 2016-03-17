package tryp
package droid
package view

import reflect.macros.blackbox

import cats._
import cats.data._
import cats.syntax.foldable._
import Func._

import scalaz.Liskov.<~<

import simulacrum._

import iota._

class IotaM(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  def contextKestrel[A, B](f: Expr[A => B])
  : Expr[Context => AppFunc[IO, A, A]] = {
    Expr {
      q"""
      { (ctx: $actx) =>
        implicit val ${TermName(c.freshName)} = ctx
        iota.kestrel($f)
      }
      """
    }
  }
}

trait IotaAnnBase
extends SimpleMethodAnnotation
with AndroidMacros
{
  val c: blackbox.Context

  import c.universe._

  def templ(ann: MethodAnnottee, wrap: Boolean) = {
    val m = ann.method
    MethodAnnottee {
      val impl =
        if(wrap) List(q"val x = ${m.rhs}", q"x(ctx)")
        else List(q"iota.kestrel[Principal, Any](${m.rhs})")
      q"""
      def ${m.name}[..${m.tparams}](...${m.vparamss}): CK[Principal] =
        (ctx: $actx) => {
          implicit val ${TermName(c.freshName)} = ctx
          ..$impl
        }
      """
    }
  }

  def templF(ann: MethodAnnottee, wrap: Boolean) = {
    val ann0 = ann.withRhs {
      q"""
      foldableSyntaxU(${ann.rhs}).fold
      """
    }
    templ(ann0, wrap)
  }
}

class IotaAnn(val c: blackbox.Context)
extends IotaAnnBase
{
  def apply(ann: MethodAnnottee) = templ(ann, false)
}

class IotaAnnWrap(val c: blackbox.Context)
extends IotaAnnBase
{
  def apply(ann: MethodAnnottee) = templ(ann, true)
}

class IotaAnnWrapFold(val c: blackbox.Context)
extends IotaAnnBase
{
  def apply(ann: MethodAnnottee) = templF(ann, true)
}

@anno(IotaAnn) class ck()

@anno(IotaAnnWrap) class ckw()

@anno(IotaAnnWrapFold) class ckwf()

import scalaz.Liskov.<~<

@typeclass trait ChainKestrel[F[_]]
{
  @op(">>-", alias = true)
  def chain[A, B, G[_]](fa: F[A])(gb: G[B])
  (implicit lis: A <~< B, other: ChainKestrel[G]): CK[A] = {
    val c1 = curry(fa)
    val c2 = other.curry(gb)
    (ctx: Context) => {
      val (k1, k2) = (c1(ctx), c2(ctx))
      k1 >=> { a => k2(lis(a)); IO(a) }
    }
  }

  def curry[A](fa: F[A]): CK[A]
}

trait ChainKestrelInstances
{
  implicit lazy val chainPlainKestrel = new ChainKestrel[Kestrel] {

    def curry[A](fa: Kestrel[A]) = (ctx: Context) => fa
  }

  implicit def chainContextKestrel =
    new ChainKestrel[CK] {
      def curry[A](fa: CK[A]) = fa
    }
}

object ChainKestrel
extends ChainKestrelInstances
{
  def ckZero[A]: CK[A] = (ctx: Context) => kestrel((_: A) => ())
}

object CKInstances
{
  import ChainKestrel.ops._

  implicit def ckMonoid[A] = new Monoid[CK[A]] {
    def empty = ChainKestrel.ckZero[A]

    def combine(x: CK[A], y: CK[A]) = x >>- y
  }
}

final class KestrelOps[A](fa: Kestrel[A])
{
  def ck[B <: A]: CK[B] = {
    Con(ctx => a => { fa(a); IO(a) })
  }

  def unit[B <: A]: B => Unit = a => { fa(a); () }
}

trait CombinatorBase
{
  def k[A, B](f: Context => A => B): ConIOF[A, A] =
    Con(ctx => a => iota.IO { f(ctx)(a); a })

  implicit def ToKestrelOps[A](fa: Kestrel[A]) = new KestrelOps(fa)
}

trait IotaCombinators[P]
extends CombinatorBase
{
  protected type Principal = P

  protected implicit def ckMonoid[P] = CKInstances.ckMonoid[P]

  protected def kk[A <: P, B](f: Principal => B) = {
    k[A, B](ctx => a => f(a: P))
  }

  protected def kp[B](f: Principal => B) = {
    kk[P, B](f)
  }

  protected def nopK[A <: P] = kk[A, Unit](_ => ())

  def resK[A <: P, B](res: Throwable Xor B)(impl: B => P => Unit): CK[A] = {
    res
      .map(r => kk[A, Unit](impl(r)))
      .getOrElse(nopK[A])
  }
}
