package tryp
package droid
package view
package core

import reflect.macros.blackbox

import cats._
import cats.syntax.foldable._

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
  (implicit ev: B <:< A): Kestrel[B, C, F] = {
    K[B, C, F] { b =>
      ConsAIO[F].pure[B, C] { c =>
        strip[C](ev(b))(c)
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
