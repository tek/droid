package tryp
package droid
package view
package core

import reflect.macros.blackbox

import cats._
import cats.syntax.foldable._

import iota.effect._

trait CKAnnBase
extends SimpleMethodAnnotation
with MacroMetadata
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
  def templ(ann: MethodAnnottee, wrap: Boolean, asString: Option[String]) = {
    val m = ann.method
    val tpe = kestrelType(m)
    val rhsTpe =
      if (wrap) tq"Kestrel[$tpe, Context, AIO]"
      else tq"$tpe => Unit"
    MethodAnnottee {
      val impl =
        if(wrap) List(q"val x = ${m.rhs}", q"{ (a: $tpe) => x(a)(ctx) }")
        else List(q"${m.rhs}")
      val invoke =
        if(wrap) q"kst(a)(ctx)"
        else q"kst(a)"
      val content = asString | ann.rhs.toString
      q"""
      def ${m.name}[..${m.tparams}](...${m.vparamss})
      : Kestrel[$tpe, Context, AIO] =
        DescribedKestrel(${m.name.toString}, $content, {
          a => AIO(
            implicit ctx => {
              val kst: $rhsTpe = { ..$impl }
              $invoke
              a
            }, $content)
        })
      """
    }
  }

  def templF(ann: MethodAnnottee, wrap: Boolean) = {
    val asString = ann.rhs.toString
    val ann0 = ann.withRhs {
      q"""
      cats.syntax.foldable.catsSyntaxUFoldable(${ann.rhs}).fold
      """
    }
    templ(ann0, wrap, Some(asString))
  }
}

class CKAnn(val c: blackbox.Context)
extends CKAnnBase
{
  def apply(ann: MethodAnnottee) = templ(ann, false, None)
}

class CKAnnWrap(val c: blackbox.Context)
extends CKAnnBase
{
  def apply(ann: MethodAnnottee) = templ(ann, true, None)
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

final class CKIotaKestrelOps[A, F[_, _]: ConsAIO](fa: iota.effect.Kestrel[A])
extends ToIotaKestrelOps
{
  def ck = fa.liftAs[A, Context, F]
}

trait ToCKIotaKestrelOps
{
  protected implicit def ToCKIotaKestrelOps[A](fa: iota.effect.Kestrel[A]) =
    new CKIotaKestrelOps[A, AIO](fa)
}

trait CKCombinators
extends ToIotaKestrelOps
with ResourcesAccess
with Logging
with ToCKIotaKestrelOps
{
  def kraw[A, B](f: Context => A => B): CK[A] =
    CK(a => AIO(ctx => { f(ctx)(a); a }, "from CKCombinators.kraw"))
}

trait Combinators[P]
extends CKCombinators
with cats.instances.FunctionInstances
{
  protected type Principal = P

  protected implicit def IotaKestrelToCK[A >: Principal](
    fa: iota.effect.Kestrel[A]) =
    fa.ck

  protected def kkpsub[A <: P, B](f: Principal => B): CK[A] =
    CK.lift[A, B, AIO](f)

  protected def k[B](f: Principal => B) = {
    kkpsub[P, B](f)
  }

  protected def ksub[A <: P, B](f: A => B): CK[A] =
    CK.lift[A, B, AIO](f)

  def nopK: CK[P] = k(_ => ())

  protected def nopKSub[A <: P]: CK[A] = ksub(_ => ())

  def resK[A <: P, B](res: Throwable Either B)(impl: B => P => Unit): CK[A] = {
    res
      .map(r => ksub[A, Unit](impl(r)))
      .getOrElse(nopKSub[A])
  }
}

trait ViewCombinators
extends Combinators[View]
