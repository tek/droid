package tryp
package droid
package view

import reflect.macros._

import iota._

import simulacrum._

import shapeless._
import shapeless.ops.hlist._

@typeclass trait IOBuilder[F[_]]
{
  @op(">>-", alias = true)
  def flatMap[A, B]
  (fa: F[A])
  (f: ConIOF[A, B])
  : F[B] =
  {
    lift { ctx =>
      ctor(fa)(ctx) >>= f(ctx)
    }
  }

  @op(">>=", alias = true)
  def flatMapK[A, B]
  (fa: F[A])
  (f: A => IO[B])
  : F[B] =
  {
    flatMap(fa)(Con(c => f))
  }

  def reify[A](fa: F[A])(implicit ctx: Context): IO[A] = {
    ctor(fa)(ctx)
  }

  def perform[A](fa: F[A])()(implicit ctx: Context): A = {
    reify(fa).perform()
  }

  def performMain[A](fa: F[A])()(implicit ctx: Context): Future[A] = {
    reify(fa).performMain()
  }

  def lift[A, B](f: Context => iota.IO[B]) = pure(Con(f))

  def pure[A](f: ConIO[A]): F[A]

  def ctor[A](fa: F[A]): ConIO[A]
}

trait IOBase[A]
{
  def ctor: ConIO[A]
}

case class Widget[A <: View](ctor: ConIO[A])
extends IOBase[A]

case class Layout[A <: ViewGroup](ctor: ConIO[A])
extends IOBase[A]

case class SimpleIO[A](ctor: ConIO[A])

object SimpleIO
{
  implicit def simpleIoBuilder = new IOBuilder[SimpleIO] {
    def pure[A](f: ConIO[A]) = SimpleIO(f)

    def ctor[A](fa: SimpleIO[A]) = fa.ctor
  }
}

object reifySub
extends Poly1
{
  implicit def caseIOBuilder[A <: View, F[_]](implicit b: IOBuilder[F]) =
    at[F[A]](fa => (ctx: Context) => b.reify(fa)(ctx))
}

case class LayoutBuilder[A, F[_]](layout: ConIOF[List[IOV], A])
(implicit builder: IOBuilder[F])
{
  def apply[In <: HList, Out <: HList](vs: In)
  (implicit
    m: Mapper.Aux[reifySub.type, In, Out],
    tta: ToTraversable.Aux[Out, List, Context => IOV]): F[A] = {
      val f = { ctx: Context =>
        val reifies = vs.map(reifySub).toList.map(_(ctx))
        layout(ctx)(reifies)
      }
      builder.pure(Con(f))
  }
}

trait Views
{
  def w[A <: View]: SimpleIO[A] = macro ViewM.w[A, SimpleIO]

  def l[A <: ViewGroup](inner: Any): SimpleIO[A] = macro ViewM.l[A, SimpleIO]
}

class ViewM(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  def w[A <: View: WeakTypeTag, F[_]]
  (implicit wtf: WeakTypeTag[F[_]]): Expr[F[A]] = {
    val aType = weakTypeOf[A]
    val TypeRef(pre, sym, args) = wtf.tpe
    val ctor = internal.typeRef(pre, sym, List(aType))
    Expr[F[A]] {
      q"""
      new $ctor(tryp.droid.view.Con(iota.w[$aType](_)))
      """
    }
  }

  def l[A <: ViewGroup: c.WeakTypeTag, F[_]](inner: Expr[Any])
  (implicit wtf: WeakTypeTag[F[_]]): Expr[F[A]] = {
    val aType = weakTypeOf[A]
    val fType = wtf.tpe.typeConstructor
    Expr {
      q"""
      iota.c[$aType] {
        val b = tryp.droid.view.LayoutBuilder[$aType, $fType](
          tryp.droid.view.Con(ctx => sub => iota.l[$aType](sub: _*)(ctx)))
        b($inner)
      }
      """
    }
  }
}
