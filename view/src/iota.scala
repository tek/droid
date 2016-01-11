package tryp
package droid
package view

import reflect.macros.blackbox

import cats._
import cats.data._
import Func._

import simulacrum._

import iota._

class IotaM(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  def contextKestrel[A, B](f: Expr[A ⇒ B])
  : Expr[Context ⇒ AppFunc[IO, A, A]] = {
    Expr {
      q"""
      { (ctx: $actx) ⇒
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
        if(wrap) m.rhs
        else q"iota.kestrel[Principal, Any](${m.rhs})"
      q"""
      def ${m.name}[..${m.tparams}](...${m.vparamss}): CK[Principal] =
        (ctx: $actx) ⇒ {
          implicit val ${TermName(c.freshName)} = ctx
          $impl
        }
      """
    }
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

trait IotaCombinators[A]
{
  type Principal = A

  def k[B] = kestrel[Principal, B] _
}

@anno(IotaAnn) class ck()

@anno(IotaAnnWrap) class ckw()

import scalaz.Liskov.<~<

@typeclass trait ChainKestrel[F[_]]
{
  @op(">>=", alias = true)
  def chain[A, B, G[_]](fa: F[A])(gb: G[B])
  (implicit lis: A <~< B, other: ChainKestrel[G]): CK[A] = {
    val c1 = curry(fa)
    val c2 = other.curry(gb)
    (ctx: Context) ⇒ {
      val (k1, k2) = (c1(ctx), c2(ctx))
      k1 >=> { a ⇒ k2(lis(a)); IO(a) }
    }
  }

  def curry[A](fa: F[A]): CK[A]
}

trait ChainKestrelInstances
{
  implicit lazy val chainPlainKestrel = new ChainKestrel[Kestrel] {

    def curry[A](fa: Kestrel[A]) = ctx ⇒ fa
  }

  implicit def chainContextKestrel =
    new ChainKestrel[CK] {
      def curry[A](fa: CK[A]) = fa
    }
}
