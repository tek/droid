package tryp
package droid
package view.meta

import cats._

import iota._

trait IOTypes
{
  type IOV = IO[View]
  type IOTrans[A, B] = A ⇒ IO[B]
  type IOCtor[A] = IOTrans[Context, A]
  type IOCTrans[A, B] = Context ⇒ IOTrans[A, B]
  type CF[A, B] = Context ⇒ A ⇒ B
  type CK[A] = CF[A, IO[A]]
}

trait IOInstances
{
  implicit val ioInstance = new Bimonad[IO] {
    def pure[A](x: A): IO[A] = IO(x)

    def extract[A](x: IO[A]): A = {
      x.perform()
    }

    def coflatMap[A, B](fa: IO[A])(f: IO[A] ⇒ B): IO[B] = {
      pure(f(fa))
    }

    def flatMap[A, B](fa: IO[A])(f: A ⇒ IO[B]): IO[B] = {
      fa.flatMap(f)
    }
  }
}
