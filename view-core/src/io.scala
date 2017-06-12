package tryp
package droid
package view
package core

import scala.annotation.tailrec

import android.os.{Looper, Handler}

import cats.Monoid

import concurrent.Await

import reflect.macros._

import simulacrum._

import shapeless._
import shapeless.ops.hlist._

import scalaz.Liskov._

trait ConsAIO[F[_, _]]
{
  def cons[A, C](fa: F[A, C])(c: C): A
  def init[A, C](fa: F[A, C])(c: C): A = cons[A, C](fa)(c)
  def pure[A, C](run: C => A): F[A, C]
  def pureDesc[A, C](run: C => A, desc: String): F[A, C]

  def withContext[A, C, D <: C](fa: F[A, C]): F[A, D] =
    pure[A, D](d => cons[A, C](fa)(d))
}

object ConsAIO
{
  def apply[F[_, _]](implicit instance: ConsAIO[F]): ConsAIO[F] = instance

  abstract class Ops[F[_, _], A, C]
  {
    def typeClassInstance: ConsAIO[F]
    def self: F[A, C]

    def cons(c: C): A = typeClassInstance.cons(self)(c)
    def init(c: C): A = typeClassInstance.init(self)(c)
    def withContext[D <: C]: F[A, D] = typeClassInstance.withContext(self)
  }

  trait ToConsAIOOps
  {
    implicit def toConsAIOOps[F[_, _]: ConsAIO, A, C](fa: F[A, C])
    (implicit tc: ConsAIO[F]): Ops[F, A, C] =
      new Ops[F, A, C] {
        val self = fa
        val typeClassInstance = tc
      }
    }

  object ops
  extends ToConsAIOOps
}

@tc trait DescribeAIO[F[_, _]]
{
  def desc[A, C](fa: F[A, C]): String
}

trait AIOI[A, C]
{
  def run: C => A
  def desc: String
}

case class AIO[A, C](run: C => A, desc: String)
extends AIOI[A, C]
with (C => A)
{
  def apply(c: C): A = run(c)

  override def toString = s"AIO($desc)"
}

trait Kestrel[A, C, F[_, _]]
{
  def run: A => F[A, C]

  def apply(a: A) = run(a)

  def impl: String
}

trait KestrelInstances
{
  import ChainKestrel.ops._

  implicit def kMonoid[A, C, F[_, _]]
  (implicit F: ConsAIO[F], k: ChainKestrel[Kestrel[?, C, F]])
  : Monoid[Kestrel[A, C, F]] = {
    type This = Kestrel[A, C, F]
    new Monoid[This] {
      def empty = K[A, C, F](a => F.pure(c => a))

      def combine(x: This, y: This) =
        ChainKestrel[Kestrel[?, C, F]].chain(x)(y)
    }
  }
}

trait KestrelFunctions1
{
  implicit def liftDefault[A, B, C](f: A => B): Kestrel[A, C, AIO] = {
    K[A, C, AIO](a => AIO(ctx => { f(a); a }, f.toString))
  }
}

trait KestrelFunctions
extends KestrelFunctions1
{
  implicit def lift[A, B, C, F[_, _]: ConsAIO](f: A => B): Kestrel[A, C, F] = {
    K[A, C, F](a => ConsAIO[F].pure(ctx => { f(a); a }))
  }
}

object Kestrel
extends KestrelInstances
with KestrelFunctions

case class K[A, C, F[_, _]](run: A => F[A, C])
extends Kestrel[A, C, F]
{
  def impl = run.toString
}

case class DescribedKestrel[A, C, F[_, _]](name: String, impl: String,
  run: A => F[A, C])
extends Kestrel[A, C, F]
{
  override def toString = s"${this.className}($name: $impl)"
}

@typeclass trait ChainKestrel[F[_]]
{
  /** Keeps the original value, only for side effects
   */
  @op(">-", alias = true)
  def chain[A, B, G[_]](fa: F[A])(gb: F[B])
  (implicit lis: A <~< B): F[A]

  @op(">=", alias = true)
  def chainIota[A, B, G[_]](fa: F[A])(gb: iota.effect.Kestrel[B])
  (implicit lis: A <~< B): F[A]
}

trait ChainKestrelInstances
extends ToIotaKestrelOps
{
  implicit def instance_ChainKestrel_Kestrel[C, F[_, _]]
  (implicit F: ConsAIO[F]): ChainKestrel[Kestrel[?, C, F]] = {
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

      def chainIota[A, B, G[_]](fa: KS[A])(gb: iota.effect.Kestrel[B])
      (implicit lis: A <~< B): KS[A] = {
        chain(fa)(gb.liftAs[A, C, F])
      }
    }
  }

  implicit lazy val instance_ChainKestrel_iotaKestrel =
    new ChainKestrel[iota.effect.Kestrel] {
      def chain[A, B, G[_]](fa: iota.effect.Kestrel[A])(fb: iota.effect.Kestrel[B])
      (implicit lis: A <~< B): iota.effect.Kestrel[A] = {
        a => {
          fb(lis(fa(a).perform())).perform()
          iota.effect.IO(a)
        }
      }

      def chainIota[A, B, G[_]](fa: iota.effect.Kestrel[A])(gb: iota.effect.Kestrel[B])
      (implicit lis: A <~< B): iota.effect.Kestrel[A] = {
        chain(fa)(gb)
      }
    }
}

object ChainKestrel
extends ChainKestrelInstances
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

trait AIOInstances
{
  implicit def instance_Monad_AIO[C]: Monad[AIO[?, C]] = new Monad[AIO[?, C]]
  {
    def pure[A](a: A) = AIO(c => a, a.toString)

    def flatMap[A, B](fa: AIO[A, C])(f: A => AIO[B, C]) = {
      AIO(c => f(fa.run(c)).run(c), s"${fa.desc}.flatMap($f)")
    }

    override def map[A, B](fa: AIO[A, C])(f: A => B) = {
      AIO(c => f(fa.run(c)), s"${fa.desc}.map($f)")
    }

    def tailRecM[A, B](a: A)(f: A => AIO[Either[A, B], C]): AIO[B, C] =
      f(a).flatMap {
        case Right(b) => pure(b)
        case Left(a1) => tailRecM(a1)(f)
      }
  }

  implicit lazy val instance_ConsAIO_AIO: ConsAIO[AIO] = new ConsAIO[AIO] {
    def cons[A, C](fa: AIO[A, C])(c: C): A = fa(c)
    def pure[A, C](run: C => A): AIO[A, C] = AIO(run, run.toString)
    def pureDesc[A, C](run: C => A, desc: String): AIO[A, C] = AIO(run, desc)
  }

  implicit def instance_PerformAIO_AAIO: PerformAIO[AIO] =
    new PerformAIO[AIO] {
      def perform[A, C](fa: AIO[A, C])(implicit c: C) =
        IO(fa(c))

      def performMain[A, C](fa: AIO[A, C])(implicit c: C) = PerformAIO.mainIO(fa(c))

      def performMainTimed[A, C](fa: AIO[A, C])(timeout: Duration = 5.seconds)(implicit c: C) = {
        PerformAIO.performMainTimed(IO(fa(c)), timeout)
      }
    }

  implicit def instance_DescribeAIO_AIO: DescribeAIO[AIO] =
    new DescribeAIO[AIO] {
      def desc[A, C](fa: AIO[A, C]) = fa.desc
    }
}

object AIO
extends AIOInstances

class ApplyKestrel[F[_, _]: ConsAIO: DescribeAIO]
{
  import PerformAIO.ops._
  import DescribeAIO.ops._

  def applyKestrel[A, B >: A, C, G[_, _]: ConsAIO]
  (fa: F[A, C])
  (k: Kestrel[B, C, G])
  : F[A, C] = {
    ConsAIO[F].pureDesc[A, C](combineRun[A, B, C, G](fa)(k),
      s"${fa.desc} >>- ${k.impl}")
  }

  def combineRun[A, B >: A, C, G[_, _]: ConsAIO]
  (fa: F[A, C])(k: Kestrel[B, C, G]): C => A = {
    c =>
      val a = ConsAIO[F].cons(fa)(c)
      val b = k.run(a)
      ConsAIO[G].cons(b)(c)
      a
  }


}

object ApplyKestrel
extends ToIotaKestrelOps
{
  implicit def instance_ApplyKestrel[F[_, _]: ConsAIO: DescribeAIO]
  : ApplyKestrel[F] = new ApplyKestrel

  def apply[F[_, _]](implicit instance: ApplyKestrel[F]): ApplyKestrel[F] =
    instance

  abstract class Ops[F[_, _]: ConsAIO, A, C]
  {
    def typeClassInstance: ApplyKestrel[F]
    def self: F[A, C]

    def applyKestrel[B >: A, G[_, _]: ConsAIO](ka: Kestrel[B, C, G]): F[A, C] =
      typeClassInstance.applyKestrel[A, B, C, G](self)(ka)

    def >>-[B >: A](ka: Kestrel[B, C, AIO]): F[A, C] =
      applyKestrel[B, AIO](ka)

    def >-[B >: A, G[_, _]: ConsAIO](ka: Kestrel[B, C, G]): F[A, C] =
      applyKestrel[B, G](ka)

    def >>=[B >: A](ka: iota.effect.Kestrel[B]): F[A, C] =
      applyKestrel(ka.liftAs[A, C, AIO])
  }

  trait ToApplyKestrelOps
  {
    implicit def toApplyKestrelOps[F[_, _]: ConsAIO, A, C](fa: F[A, C])
    (implicit tc: ApplyKestrel[F]): Ops[F, A, C] =
      new Ops[F, A, C] {
        val self = fa
        val typeClassInstance = tc
      }
    }

  object ops
  extends ToApplyKestrelOps
}

trait PerformAIO[F[_, _]]
{
  def perform[A, C](fa: F[A, C])(implicit c: C): IO[A]

  def performMain[A, C](fa: F[A, C])(implicit c: C): IO[A]

  def performMainTimed[A, C](fa: F[A, C])(timeout: Duration)(implicit c: C): IO[Option[A]]
}

trait PerformAIOExecution
{
  import java.util.concurrent._

  val poolQueue = new LinkedBlockingQueue[Runnable]()

  val mainName = "android main"

  object AndroidMainExecutorService
  extends PoolExecutor(mainName, "android", 1, 1, 0L, poolQueue, PoolExecutor.defaultFactory(mainName))
  {
    lazy val handler = new Handler(Looper.getMainLooper)

    override def execute(r: Runnable) = handler.post(r)

    override def submit[A](c: Callable[A]) = {
      val fut = new FutureTask[A](c)
      execute(fut)
      fut
    }
  }

  val androidEC = ExecutionContext.fromExecutor(AndroidMainExecutorService)

  def performMain[A](io: IO[A]): IO[A] = IO.shift(androidEC) >> io

  def performMainTimed[A](io: IO[A], timeout: Duration): IO[Option[A]] = {
    IO.shift(androidEC) >> IO(io.unsafeRunTimed(timeout))
  }

  def mainIO[A](f: => A): IO[A] = {
    performMain(IO(f))
  }

  object AndroidMainExecutionContext
  extends java.util.concurrent.ForkJoinPool
  with concurrent.ExecutionContextExecutorService
  {
    val handler = new Handler(Looper.getMainLooper)
    override def execute(runnable: Runnable) = handler.post(runnable)
    override def reportFailure(t: Throwable) = Log.e(t)
  }
}

object PerformAIO
extends PerformAIOExecution
{
  def apply[F[_, _], A, C](implicit instance: PerformAIO[F]) =
    instance

  abstract class Ops[F[_, _], A, C]
  {
    def typeClassInstance: PerformAIO[F]
    def self: F[A, C]

    def perform(implicit c: C): IO[A] = {
      typeClassInstance.perform[A, C](self)
    }

    def performMain(implicit c: C): IO[A] = {
      typeClassInstance.performMain[A, C](self)
    }

    def performMainTimed(timeout: Duration)(implicit c: C): IO[Option[A]] = {
      typeClassInstance.performMainTimed[A, C](self)(timeout)
    }

    def mainUnit(implicit c: C): IO[Unit] = performMain.void

    def mainUnitTimed(timeout: Duration)(implicit c: C): IO[Unit] =
      performMainTimed(timeout).void
  }

  trait ToPerformAIOOps
  {
    implicit def toPerformAIOOps[F[_, _], A, C](fa: F[A, C])
    (implicit tc: PerformAIO[F]): Ops[F, A, C] =
      new Ops[F, A, C] {
        val self = fa
        val typeClassInstance = tc
      }
    }

  object ops
  extends ToPerformAIOOps
}

trait CKFunctions
{
  implicit def liftF[A](f: A => AIO[A, Context]) =
    CK[A](f)

  def apply[A](run: A => AIO[A, Context]) = K(run)

  def lift[A, B, F[_, _]: ConsAIO](f: A => B): CK[A] = {
    liftF(a => AIO(ctx => { f(a); a }, "from CKFunctions.lift"))
  }

  def nopK[A, F[_, _]: ConsAIO]: CK[A] = lift[A, Unit, F](_ => ())
}

object CK
extends CKFunctions

object consChildren
extends Poly1
{
  implicit def caseConsAIO[A, C, F[_, _]]
  (implicit ac: ConsAIO[F]) =
    at[F[A, C]](fa => (c: C) => ac.init(fa)(c))
}

case class LayoutBuilder[A, C, F[_, _]: ConsAIO]
(layout: C => List[iota.effect.IO[View]] => A)
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
          .map(iota.effect.IO(_))
        layout(ctx)(children)
      }
      ConsAIO[F].pure(f)
  }
}

trait Views[C, F[_, _]]
{
  def w[A]: F[A, C] = macro ViewM.w[A, C, F]

  def l[A](inner: Any*): F[A, C] = macro ViewM.l[A, C, F]

  def inf[A]: F[A, C] = macro ViewM.inf[A, C, F]
}

class ViewM(val c: whitebox.Context)
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
    val cons = symbolOf[ConsAIO[F]].companion
    Expr[F[A, C]] {
      q"""
      $cons[$fSym].pure[$aType, $cType]((ctx: $cType) => new $aType(ctx))
      """
    }
  }

  def l[A: WeakTypeTag, C: WeakTypeTag, F[_, _]](inner: Expr[Any]*)
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
      iota.effect.c[$aType] {
        val b = $builder[$aType, $cType, $fSym](
          ctx => sub => iota.effect.l[$aType](sub: _*)(ctx).perform())
        b($sub)
      }
      """
    }
  }

  def inf[A: WeakTypeTag, C: WeakTypeTag, F[_, _]]
  (implicit wtf: WeakTypeTag[F[_, _]])
  : Expr[F[A, C]] = {
    val aType = weakTypeOf[A]
    val vtree = aType.typeSymbol.companion
    val cType = weakTypeOf[C]
    val vg = typeOf[ViewGroup]
    val TypeRef(_, fSym, _) = wtf.tpe
    val cons = symbolOf[ConsAIO[F]].companion
    Expr[F[A, C]] {
      q"""
      $cons[$fSym].pure[$aType, $cType]((ctx: $cType) =>
          iota.ViewTree.inflate(ctx, $vtree))
      """
    }
  }
}

trait AIOOrphans
{
  implicit def instance_Contravariant_AIO[F[_, _]: ConsAIO, A]
  : Contravariant[F[A, ?]] =
    new Contravariant[F[A, ?]] {
      def contramap[C, D](fa: F[A, C])(f: D => C): F[A, D] =
        ConsAIO[F].pure(d => ConsAIO[F].cons(fa)(f(d)))
    }
}

trait AIOViews
extends Views[Context, AIO]

object AIOViews
extends AIOViews

trait ToAIO
{
  implicit def viewToAIO[A <: View](a: A): AIO[A, Context] = {
    ConsAIO[AIO].pure[A, Context](_ => a)
  }

  implicit def viewToApplyKestrel[A <: View](a: A)
  : ApplyKestrel.Ops[AIO, A, Context] = {
    import ApplyKestrel.ops._
    viewToAIO(a)
  }
}
