package tryp
package droid
package view
package core

import android.os.{Looper, Handler}

import algebra.Monoid

import cats.Unapply

import concurrent.Await

import reflect.macros._

import simulacrum._

import shapeless._
import shapeless.ops.hlist._

import scalaz.concurrent.Task
import scalaz.Liskov._

trait ConsIO[F[_, _]]
{
  def cons[A, C](fa: F[A, C])(c: C): A
  def pure[A, C](run: C => A): F[A, C]
}

object ConsIO
{
  def apply[F[_, _]](implicit instance: ConsIO[F]): ConsIO[F] = instance
}

case class IO[A, C](run: C => A)
{
  def apply(implicit c: C): A = run(c)
}

trait Kestrel[A, C, F[_, _]]
{
  def run: A => F[A, C]

  def apply(a: A) = run(a)
}

trait KestrelInstances
{
  import ChainKestrel.ops._

  implicit def kMonoid[A, C, F[_, _]]
  (implicit F: ConsIO[F], kk: ChainKestrel[Kestrel[?, C, F]])
  : Monoid[Kestrel[A, C, F]] = {
    type This = Kestrel[A, C, F]
    new Monoid[This] {
      def empty = K[A, C, F](a => F.pure(c => a))

      def combine(x: This, y: This) =
        ChainKestrel[Kestrel[?, C, F]].chain(x)(y)
    }
  }
}

trait KestrelFunctions
{
  implicit def lift[A, B, C, F[_, _]: ConsIO](f: A => B): Kestrel[A, C, F] = {
    K[A, C, F](a => ConsIO[F].pure(ctx => { f(a); a }))
  }
}

object Kestrel
extends KestrelInstances
with KestrelFunctions

case class K[A, C, F[_, _]](run: A => F[A, C])
extends Kestrel[A, C, F]

@typeclass trait ChainKestrel[F[_]]
{
  /** Keeps the original value, only for side effects
   */
  @op(">-", alias = true)
  def chain[A, B, G[_]](fa: F[A])(gb: F[B])
  (implicit lis: A <~< B): F[A]

  @op(">=", alias = true)
  def chainIota[A, B, G[_]](fa: F[A])(gb: iota.Kestrel[B])
  (implicit lis: A <~< B): F[A]
}

trait ChainKestrelInstances
extends ToIotaKestrelOps
{
  implicit def instance_ChainKestrel_Kestrel[C, F[_, _]]
  (implicit F: ConsIO[F]): ChainKestrel[Kestrel[?, C, F]] = {
    type KS[A] = Kestrel[A, C, F]
    new ChainKestrel[KS] {
      def chain[A, B, G[_]](fa: KS[A])(fb: KS[B])
      (implicit lis: A <~< B): KS[A] =
      {
        K { a =>
          F.pure[A, C] { c =>
            val o1 = fa.run(a)
            val r1 = F.cons(o1)(c)
            val o2 = fb.run(lis(r1))
            F.cons(o2)(c)
            a
          }
        }
      }

      def chainIota[A, B, G[_]](fa: KS[A])(gb: iota.Kestrel[B])
      (implicit lis: A <~< B): KS[A] = {
        chain(fa)(gb.liftAs[A, C, F])
      }
    }
  }

  implicit lazy val instance_ChainKestrel_iotaKestrel =
    new ChainKestrel[iota.Kestrel] {
      def chain[A, B, G[_]](fa: iota.Kestrel[A])(fb: iota.Kestrel[B])
      (implicit lis: A <~< B): iota.Kestrel[A] = {
        a => {
          fb(lis(fa(a).perform())).perform()
          iota.IO(a)
        }
      }

      def chainIota[A, B, G[_]](fa: iota.Kestrel[A])(gb: iota.Kestrel[B])
      (implicit lis: A <~< B): iota.Kestrel[A] = {
        chain(fa)(gb)
      }
    }
}

object ChainKestrel
{
  implicit def instance_Unapply_ChainKestrel_Kestrel[B, C, F[_, _]]
  (implicit tc: ChainKestrel[Kestrel[?, C, F]])
  : Unapply[ChainKestrel, Kestrel[B, C, F]] =
    new Unapply[ChainKestrel, Kestrel[B, C, F]] {
      type M[X] = Kestrel[X, C, F]
      type A = B
      def TC: ChainKestrel[Kestrel[?, C, F]] = tc
      def subst: Kestrel[B, C, F] => M[A] = identity
    }

  implicit def chainKestrelOpsU[FA](fa: FA)
  (implicit U: Unapply[ChainKestrel, FA]): ChainKestrel.Ops[U.M, U.A] =
    new ChainKestrel.Ops[U.M, U.A]
    {
      val self = U.subst(fa)
      val typeClassInstance = U.TC
    }
}

trait IOInstances
{
  implicit def instance_Monad_IO[C]: Monad[IO[?, C]] = new Monad[IO[?, C]]
  {
    def pure[A](a: A) = IO(c => a)

    def flatMap[A, B](fa: IO[A, C])(f: A => IO[B, C]) = {
      IO(c => f(fa.run(c)).run(c))
    }
  }

  implicit lazy val instance_ConsIO_IO = new ConsIO[IO] {
    def cons[A, C](fa: IO[A, C])(c: C): A = fa(c)
    def pure[A, C](run: C => A): IO[A, C] = IO(run)
  }

  implicit lazy val instance_PerformIO_IO =
    new PerformIO[IO] {
      def unsafePerformIO[A, C](fa: IO[A, C])(implicit c: C) = Task(fa(c))

      def main[A, C](fa: IO[A, C])(timeout: Duration = Duration.Inf)
      (implicit c: C) = {
        PerformIO.mainTask(fa(c), timeout)
        // PerformIO.mainFuture(fa(c), timeout)
      }
    }

  implicit def instance_ApplyKestrel_IO =
    new ApplyKestrel[IO] {
      def combineRun[A, B >: A, C](fa: IO[A, C])(fb: B => IO[B, C]): C => A =
      { c =>
        val a = fa.run(c)
        fb(a).run(c)
        a
      }
    }
}

object IO
extends IOInstances

abstract class ApplyKestrel[F[_, _]: PerformIO: ConsIO]
{
  import PerformIO.ops._

  def applyKestrel[A, B >: A, C](fa: F[A, C])(k: Kestrel[B, C, F]): F[A, C] = {
    ConsIO[F].pure[A, C](combineRun[A, B, C](fa)(k.run))
  }

  def combineRun[A, B >: A, C](fa: F[A, C])(fb: B => F[B, C]): C => A
}

object ApplyKestrel
extends ToIotaKestrelOps
{
  def apply[F[_, _]](implicit instance: ApplyKestrel[F]): ApplyKestrel[F] =
    instance

  abstract class Ops[F[_, _]: ConsIO, A, C]
  {
    def typeClassInstance: ApplyKestrel[F]
    def self: F[A, C]

    def apk[B >: A](ga: Kestrel[B, C, F]) =
      typeClassInstance.applyKestrel[A, B, C](self)(ga)

    def >>-[B >: A](oka: Kestrel[B, C, F]) =
      apk(oka)

    def >>=[B >: A](ka: iota.Kestrel[B]) =
      apk(ka.liftAs[A, C, F])
  }

  trait ToApplyKestrelOps
  {
    implicit def toApplyKestrelOps[F[_, _]: ConsIO, A, C](fa: F[A, C])
    (implicit tc: ApplyKestrel[F]) =
      new Ops[F, A, C] {
        val self = fa
        val typeClassInstance = tc
      }
    }

  object ops
  extends ToApplyKestrelOps
}

trait PerformIO[F[_, _]]
{
  def unsafePerformIO[A, C](fa: F[A, C])(implicit c: C): Task[A]

  def main[A, C](fa: F[A, C])(timeout: Duration = Duration.Inf)
  (implicit c: C): Task[A]
}

trait PerformIOExecution
{
  import java.util.concurrent._

  val poolQueue = new LinkedBlockingQueue[Runnable]()

  object AndroidMainExecutorService
  extends PoolExecutor("android main", "android", 1, 1, 0L, poolQueue)
  {
    lazy val handler = new Handler(Looper.getMainLooper)

    override def execute(r: Runnable) = handler.post(r)

    override def submit[A](c: Callable[A]) = {
      val fut = new FutureTask[A](c)
      execute(fut)
      fut
    }
  }

  def main[A](task: Task[A], timeout: Duration) = {
    val timed = timeout match {
      case Duration.Inf => task
      case _ => task.unsafePerformTimed(timeout)
    }
    Task.fork(timed)(AndroidMainExecutorService)
  }

  def mainTask[A](f: => A, timeout: Duration): Task[A] = {
    main(Task(f)(AndroidMainExecutorService), timeout)
  }

  object AndroidMainExecutionContext
  extends java.util.concurrent.ForkJoinPool
  with concurrent.ExecutionContextExecutorService
  {
    val handler = new Handler(Looper.getMainLooper)
    override def execute(runnable: Runnable) = handler.post(runnable)
    override def reportFailure(t: Throwable) = Log.e(t)
  }

  def mainFuture[A](f: => A, timeout: Duration): Task[A] = {
    Task {
      Await.result(
        ScalaFuture(f)(AndroidMainExecutionContext), timeout)
    }
  }
}

object PerformIO
extends PerformIOExecution
{
  def apply[F[_, _], A, C](implicit instance: PerformIO[F]) =
    instance

  abstract class Ops[F[_, _], A, C]
  {
    def typeClassInstance: PerformIO[F]
    def self: F[A, C]

    def unsafePerformIO(implicit c: C): Task[A] = {
      typeClassInstance.unsafePerformIO[A, C](self)
    }

    def main(timeout: Duration = Duration.Inf)(implicit c: C): Task[A] = {
      typeClassInstance.main[A, C](self)(timeout)
    }

    def mainUnit(timeout: Duration = Duration.Inf)(implicit c: C): Task[Unit] =
    {
      main(timeout).void
    }
  }

  trait ToPerformIOOps
  {
    implicit def toPerformIOOps[F[_, _], A, C](fa: F[A, C])
    (implicit tc: PerformIO[F]): Ops[F, A, C] =
      new Ops[F, A, C] {
        val self = fa
        val typeClassInstance = tc
      }
    }

  object ops
  extends ToPerformIOOps
}

trait CKFunctions
{
  implicit def liftF[A, F[_, _]: ConsIO](f: A => F[A, Context]) =
    CK[A, F](f)

  def apply[A, F[_, _]](run: A => F[A, Context]) = K(run)

  def lift[A, B, F[_, _]: ConsIO](f: A => B): CK[A, F] = {
    liftF(a => ConsIO[F].pure(ctx => { f(a); a }))
  }

  def nopK[A, F[_, _]: ConsIO]: CK[A, F] = lift[A, Unit, F](_ => ())
}

object CK
extends CKFunctions

object consChildren
extends Poly1
{
  implicit def caseConsIO[A, C, F[_, _]]
  (implicit ac: ConsIO[F]) =
    at[F[A, C]](fa => (c: C) => ac.cons(fa)(c))
}

case class LayoutBuilder[A, C, F[_, _]: ConsIO]
(layout: C => List[iota.IO[View]] => A)
{
  def apply[In <: HList, Out <: HList](vs: In)
  (implicit
    m: Mapper.Aux[consChildren.type, In, Out],
    tta: ToTraversable.Aux[Out, List, C => View]
    ): F[A, C] = {
      val f = { ctx: C =>
        val children = vs
          .map(consChildren)
          .toList
          .map(_(ctx))
          .map(iota.IO(_))
        layout(ctx)(children)
      }
      ConsIO[F].pure(f)
  }
}

trait Views[C, F[_, _]]
{
  def w[A]: F[A, C] = macro ViewM.w[A, C, F]

  def l[A](inner: Any*): F[A, C] = macro ViewM.l[A, C, F]
}

class ViewM(val c: blackbox.Context)
extends AndroidMacros
{
  import c.universe._
  import c.Expr

  def w[A: WeakTypeTag, C: WeakTypeTag, F[_, _]]
  (implicit wtf: WeakTypeTag[F[_, _]])
  : Expr[F[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val TypeRef(_, fSym, _) = wtf.tpe
    val cons = symbolOf[ConsIO[F]].companion
    Expr[F[A, C]] {
      q"""
      $cons[$fSym].pure[$aType, $cType]((ctx: $cType) => new $aType(ctx))
      """
    }
  }

  def l[A: c.WeakTypeTag, C: WeakTypeTag, F[_, _]]
  (inner: Expr[Any]*)
  (implicit wtf: WeakTypeTag[F[_, _]])
  : Expr[F[A, C]] = {
    val aType = weakTypeOf[A]
    val cType = weakTypeOf[C]
    val TypeRef(_, fSym, _) = wtf.tpe
    val sub = inner.foldLeft(q"shapeless.HNil": Tree) {
      case (z, v) => q"$v :: $z"
    }
    val builder = symbolOf[LayoutBuilder[A, C, F]].companion
    Expr[F[A, C]] {
      q"""
      iota.c[$aType] {
        val b = $builder[$aType, $cType, $fSym](
          ctx => sub => iota.l[$aType](sub: _*)(ctx).perform())
        b($sub)
      }
      """
    }
  }
}