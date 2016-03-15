package tryp
package droid
package view

import reflect.macros._

import iota._

import simulacrum._

import shapeless._
import shapeless.ops.hlist._

trait AndroidEffect[F[_, _]]
{
  def curry[A, B](f: F[A, IO[B]]): IOCTrans[A, B]
}

object AndroidEffect
{
  implicit lazy val func1AndroidEffect = new AndroidEffect[Function1] {
    def curry[A, B](f: A => IO[B]) = c => f
  }

  implicit lazy val ctransAndroidEffect =
    new AndroidEffect[λ[(a, b) => Context => a => b]]
    {
      def curry[A, B](f: Context => A => IO[B]): IOCTrans[A, B] = f
    }
}

@typeclass trait IOBuilder[F[_]]
{
  @op(">>=", alias = true)
  def flatMap[A, B, C[_, _], D]
  (fa: F[A])
  (f: C[D, IO[B]])
  (implicit eff: AndroidEffect[C], lis: scalaz.Liskov[A, D]): F[B] =
  {
    lift { ctx =>
      val curried = lis.apply _ andThen eff.curry(f)(ctx)
      ctor(fa)(ctx) >>= curried
    }
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

  def lift[A, B](f: Context => iota.IO[B]) = pure(f)

  def pure[A](f: IOCtor[A]): F[A]

  def ctor[A](fa: F[A]): IOCtor[A]
}

trait IOBase[A]
{
  def ctor: IOCtor[A]
}

case class Widget[A <: View](ctor: IOCtor[A])
extends IOBase[A]

case class Layout[A <: ViewGroup](ctor: IOCtor[A])
extends IOBase[A]

case class SimpleIO[A](ctor: IOCtor[A])

object SimpleIO
{
  implicit def simpleIoBuilder = new IOBuilder[SimpleIO] {
    def pure[A](f: IOCtor[A]) = SimpleIO(f)

    def ctor[A](fa: SimpleIO[A]) = fa.ctor
  }
}

object reifySub
extends Poly1
{
  implicit def caseIOBuilder[A <: View, F[_]](implicit b: IOBuilder[F]) =
    at[F[A]](fa => (ctx: Context) => b.reify(fa)(ctx))
}

case class LayoutBuilder[A, F[_]](layout: Context => List[IOV] => IO[A])
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
      builder.pure(f)
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
      new $ctor(iota.w[$aType](_))
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
          ctx => sub => iota.l[$aType](sub: _*)(ctx))
        b($inner)
      }
      """
    }
  }
}
