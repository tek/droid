package tryp
package droid
package view
package core

import reflect.macros.blackbox

import algebra.Monoid

import cats._
import cats.syntax.foldable._

import iota._

trait CKAnnBase
extends SimpleMethodAnnotation
with AndroidMacros
{
  val c: blackbox.Context

  import c.universe._

  /* if the supplied TypeDef is a subtype of View, like A <: FrameLayout,
   * return it as Some
   */
  def extractViewType(tp: TypeDef) = {
    val checked = c.Expr(c.typecheck(tp.rhs))
    val isView = checked.actualType.baseType(symbolOf[View]) != NoType
    isView.opt(tp)
  }

  def kestrelType(m: DefDef) = {
    val supplied = m.tparams.headOption
      .flatMap(extractViewType)
    supplied.map(_.name.toTypeName) | TypeName("Principal")
  }

  /**
   * Surround a method's rhs with Kestrel construction and supply an implicit
   * Context as a val `ctx`.
   */
  def templ(ann: MethodAnnottee, wrap: Boolean) = {
    val m = ann.method
    val tpe = kestrelType(m)
    val rhsTpe = m.rhs match {
      case q"(..$params) => $rhs" =>
        tq"$tpe => Unit"
      case _ =>
        tq"Kestrel[$tpe, Context, IOT]"
    }
    MethodAnnottee {
      val impl =
        if(wrap) List(q"val x = ${m.rhs}", q"x(ctx)")
        else List(q"${m.rhs}")
      q"""
      def ${m.name}[..${m.tparams}](...${m.vparamss})
      : Kestrel[$tpe, Context, IOT] =
        K {
          a => ConsIO[IOT].pure(
            implicit ctx => {
              val kst: $rhsTpe = { ..$impl }
              kst(a)
              a
            })
        }
      """
    }
  }

  def templF(ann: MethodAnnottee, wrap: Boolean) = {
    val ann0 = ann.withRhs {
      q"""
      cats.syntax.foldable.foldableSyntaxU(${ann.rhs}).fold
      """
    }
    templ(ann0, wrap)
  }
}

class CKAnn(val c: blackbox.Context)
extends CKAnnBase
{
  def apply(ann: MethodAnnottee) = templ(ann, false)
}

class CKAnnWrap(val c: blackbox.Context)
extends CKAnnBase
{
  def apply(ann: MethodAnnottee) = templ(ann, true)
}

class CKAnnFold(val c: blackbox.Context)
extends CKAnnBase
{
  def apply(ann: MethodAnnottee) = templF(ann, false)
}

class CKAnnWrapFold(val c: blackbox.Context)
extends CKAnnBase
{
  def apply(ann: MethodAnnottee) = templF(ann, true)
}

class CKAnnResources(val c: blackbox.Context)
extends CKAnnBase
{
  def apply(ann: MethodAnnottee) = templF(ann, true)
}

object annotation
{
  @anno(CKAnn) class context()

  @anno(CKAnnFold) class contextfold()

  @anno(CKAnnWrap) class contextwrap()

  @anno(CKAnnWrapFold) class contextwrapfold()

  @anno(CKAnnResources) class resources()
}

final class CKIotaKestrelOps[A, B <: A, F[_, _]: ConsIO](fa: iota.Kestrel[A])
extends ToIotaKestrelOps
{
  def ck = fa.liftAs[B, Context, F]
}

abstract class CKCombinatorBase[F[_, _]: ConsIO]
extends ToIotaKestrelOps
with ResourcesAccess
with Logging
{
  def k[A, B](f: Context => A => B): CK[A, F] =
    CK(a => ConsIO[F].pure(ctx => { f(ctx)(a); a }))
}

abstract class CKCombinators[P, F[_, _]: ConsIO]
(implicit chain: ChainKestrel[Kestrel[?, Context, F]])
extends CKCombinatorBase[F]
{
  protected type Principal = P
  protected type IOT[A, B] = F[A, B]

  implicit def ToCKIotaKestrelOps[A >: Principal](fa: iota.Kestrel[A]) =
    new CKIotaKestrelOps[A, Principal, F](fa)

  implicit def IotaKestrelToCK[A >: Principal](fa: iota.Kestrel[A]) =
    fa.ck

  protected def kk[A <: P, B](f: Principal => B): CK[A, F] = {
    CK(a => ConsIO[F].pure(ctx => { f(a: P); a }))
  }

  protected def kp[B](f: Principal => B) = {
    kk[P, B](f)
  }

  protected def nopK[A <: P]: CK[A, F] = kk[A, Unit](_ => ())

  def resK[A <: P, B](res: Throwable Xor B)(impl: B => P => Unit): CK[A, F] = {
    res
      .map(r => kk[A, Unit](impl(r)))
      .getOrElse(nopK[A])
  }
}
