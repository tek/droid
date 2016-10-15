package tryp
package droid
package view
package core

import reflect.macros.blackbox

import cats._
import cats.syntax.foldable._

import scalaz.Liskov.<~<

import simulacrum._

import iota._

trait IotaOrphans
{
  import iota.IO

  implicit val ioInstance = new Bimonad[IO] {
    def pure[A](x: A): IO[A] = IO(x)

    def extract[A](x: IO[A]): A = {
      x.perform()
    }

    def coflatMap[A, B](fa: IO[A])(f: IO[A] => B): IO[B] = {
      pure(f(fa))
    }

    def flatMap[A, B](fa: IO[A])(f: A => IO[B]): IO[B] = {
      fa.flatMap(f)
    }

    def tailRecM[A, B](a: A)(f: A => IO[Either[A, B]])
    : IO[B] =
      defaultTailRecM(a)(f)
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
//         a => IO(implicit ctx => {
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

final class IotaKestrelOps[A](fa: iota.Kestrel[A])
{
  def strip[C]: A => C => A = {
    a => c => fa(a).perform()
  }

  def lift[C, F[_, _]: ConsIO]: Kestrel[A, C, F] = {
    K[A, C, F] { b =>
      ConsIO[F].pure[A, C] { c =>
        strip[C](b)(c)
        b
      }
    }
  }

  def liftAs[B, C, F[_, _]: ConsIO]
  (implicit lis: B <~< A): Kestrel[B, C, F] = {
    K[B, C, F] { b =>
      ConsIO[F].pure[B, C] { c =>
        strip[C](lis(b))(c)
        b
      }
    }
  }

  def ok[B <: A, F[_, _]: ConsIO] = liftAs[B, Context, F]

  def unit[B <: A]: B => Unit = a => { fa(a); () }
}

trait ToIotaKestrelOps
{
  implicit def ToIotaKestrelOps[A](fa: iota.Kestrel[A]) = 
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
//     CK(a => ConsIO[F].pure(ctx => { f(a: P); a }))
//   }

//   protected def kp[B](f: Principal => B) = {
//     k[P, B](f)
//   }

//   protected def nopK[A <: P]: CK[A] = k[A, Unit](_ => ())

//   def resK[A <: P, B](res: Throwable Xor B)(impl: B => P => Unit): CK[A] = {
//     res
//       .map(r => k[A, Unit](impl(r)))
//       .getOrElse(nopK[A])
//   }
// }
