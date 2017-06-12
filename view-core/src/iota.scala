package tryp
package droid
package view
package core

import reflect.macros.blackbox

import cats._
import cats.syntax.foldable._

import scalaz.Liskov.<~<

import simulacrum._

import iota.effect._

trait IotaOrphans
{
  type IIO[A] = iota.effect.IO[A]

  implicit val ioInstance = new Bimonad[IIO] {
    def pure[A](x: A): IIO[A] = iota.effect.IO(x)

    def extract[A](x: IIO[A]): A = {
      x.perform()
    }

    def coflatMap[A, B](fa: IIO[A])(f: IIO[A] => B): IIO[B] = {
      pure(f(fa))
    }

    def flatMap[A, B](fa: IIO[A])(f: A => IIO[B]): IIO[B] = {
      fa.flatMap(f)
    }

    def tailRecM[A, B](a: A)(f: A => IIO[Either[A, B]]): IIO[B] =
      f(a).flatMap {
        case Left(a1) => tailRecM(a1)(f)
        case Right(b) => pure(b)
      }
  }
}

// trait IotaAnnBase
// extends SimpleMethodAnnotation
// with AndroidMacros
// {
//   val c: blackbox.Context

//   import c.universe._

//   /* if the supplied TypeDef is a subtype of View, like A <: FrameLayout,
//    * return it as Some
//    */
//   def extractViewType(tp: TypeDef) = {
//     val checked = c.Expr(c.typecheck(tp.rhs))
//     val isView = checked.actualType.baseType(symbolOf[View]) != NoType
//     isView.opt(tp)
//   }

//   def templ(ann: MethodAnnottee, wrap: Boolean) = {
//     val m = ann.method
//     val tpe = m.tparams.headOption
//       .flatMap(extractViewType)
//     val name = tpe.map(_.name.toTypeName) | TypeName("Principal")
//     MethodAnnottee {
//       val impl =
//         if(wrap) List(q"val x = ${m.rhs}", q"x(ctx)")
//         else List(q"${m.rhs}")
//       q"""
//       def ${m.name}[..${m.tparams}](...${m.vparamss}): CK[$name] =
//         a => AIO(implicit ctx => {
//           implicit val res = tryp.droid.core.Resources.fromContext
//             val kst = {
//               ..$impl
//             }
//             kst(a)
//         })
//       """
//     }
//   }

//   def templF(ann: MethodAnnottee, wrap: Boolean) = {
//     val ann0 = ann.withRhs {
//       q"""
//       foldableSyntaxU(${ann.rhs}).fold
//       """
//     }
//     templ(ann0, wrap)
//   }
// }

// class IotaAnn(val c: blackbox.Context)
// extends IotaAnnBase
// {
//   def apply(ann: MethodAnnottee) = templ(ann, false)
// }

// class IotaAnnWrap(val c: blackbox.Context)
// extends IotaAnnBase
// {
//   def apply(ann: MethodAnnottee) = templ(ann, true)
// }

// class IotaAnnWrapFold(val c: blackbox.Context)
// extends IotaAnnBase
// {
//   def apply(ann: MethodAnnottee) = templF(ann, true)
// }

// object ann
// {
//   @anno(IotaAnn) class ck()

//   @anno(IotaAnnWrap) class ckw()

//   @anno(IotaAnnWrapFold) class ckwf()
// }

final class IotaKestrelOps[A](fa: iota.effect.Kestrel[A])
{
  def strip[C]: A => C => A = {
    a => c => fa(a).perform()
  }

  def lift[C, F[_, _]: ConsAIO]: Kestrel[A, C, F] = {
    K[A, C, F] { b =>
      ConsAIO[F].pure[A, C] { c =>
        strip[C](b)(c)
        b
      }
    }
  }

  def liftAs[B, C, F[_, _]: ConsAIO]
  (implicit lis: B <~< A): Kestrel[B, C, F] = {
    K[B, C, F] { b =>
      ConsAIO[F].pure[B, C] { c =>
        strip[C](lis(b))(c)
        b
      }
    }
  }

  def ok[B <: A, F[_, _]: ConsAIO] = liftAs[B, Context, F]

  def unit[B <: A]: B => Unit = a => { fa(a); () }
}

trait ToIotaKestrelOps
{
  implicit def ToIotaKestrelOps[A](fa: iota.effect.Kestrel[A]) =
    new IotaKestrelOps(fa)
}

// abstract class CombinatorBase
// extends ToIotaKestrelOps
// {
//   def k[A, B](f: Context => A => B): CK[A] =
//     OCK(a => ctx => { f(ctx)(a); a })
// }

// abstract class IotaCombinators[P]
// extends CombinatorBase[F]
// {
//   protected type Principal = P

//   protected def k[A <: P, B](f: Principal => B): CK[A] = {
//     CK(a => ConsAIO[F].pure(ctx => { f(a: P); a }))
//   }

//   protected def kp[B](f: Principal => B) = {
//     k[P, B](f)
//   }

//   protected def nopK[A <: P]: CK[A] = k[A, Unit](_ => ())

//   def resK[A <: P, B](res: Throwable Either B)(impl: B => P => Unit): CK[A] = {
//     res
//       .map(r => k[A, Unit](impl(r)))
//       .getOrElse(nopK[A])
//   }
// }
