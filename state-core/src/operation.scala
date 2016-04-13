package tryp
package droid
package state
package core

import scala.annotation.implicitNotFound

import scalaz.{\/, \/-, -\/, ValidationNel}

import cats._
import cats.std.all._

import export._

import Process._

@implicitNotFound("no Operation[${A}] defined")
trait Operation[A]
{
  def result(a: A): Result
}

trait ParcelOperation[A]
extends Operation[A]
{
  def result(a: A) = parcel(a).validNel[Parcel]
  def parcel(a: A): Parcel
}

final class OperationSyntax[A](a: A)(implicit op: Operation[A])
{
  def toResult = op.result(a)
}

final class ParcelOperationSyntax[A](a: A)(implicit op: ParcelOperation[A])
{
  def toParcel = op.parcel(a)
}

trait ToOperationSyntax
{
  implicit def ToOperationSyntax[A: Operation](a: A) = new OperationSyntax(a)
  implicit def ToParcelOperationSyntax[A: ParcelOperation](a: A) =
    new ParcelOperationSyntax(a)
}

@imports[Operation]
trait OperationOrphans

trait OperationInstances
extends ToOperationSyntax
with OperationOrphans
{
  implicit def messageOperation[A <: Message](implicit pf: PublishFilter[A]) =
    new ParcelOperation[A] {
      def parcel(m: A): Parcel = m
      override def toString = "Operation[Message]"
    }

  implicit def unitOperation =
    new ParcelOperation[Unit] {
      def parcel(u: Unit) = Internal(UnitTask)
      override def toString = "Operation[Unit]"
    }

  implicit def optionOperation[A: ParcelOperation]
  (implicit pf: PublishFilter[LogVerbose]) = new ParcelOperation[Option[A]] {
    def parcel(oa: Option[A]) = {
      oa.map(_.toParcel) |
        LogVerbose("empty option produced by app effect").toParcel
    }

    override def toString = "Operation[Option]"
  }

  implicit def validatedNelOperation[A: ParcelOperation, B: Show] =
    new Operation[ValidatedNel[B, A]] {
      def result(v: ValidatedNel[B, A]): Result = {
        v.bimap(_.map(e => LogError("from nel", e.show).publish), _.toParcel)
      }

      override def toString = "Operation[ValidatedNel]"
    }

  implicit def validationNelOperation[A: ParcelOperation] =
    new Operation[ValidationNel[String, A]] {
      def result(v: ValidationNel[String, A]): Result = {
        v.toValidatedNel.toResult
      }

      override def toString = "Operation[ValidationNel]"
    }

  implicit def xorOperation[A: ParcelOperation, B: Show] =
    new Operation[Xor[B, A]] {
      def result(v: Xor[B, A]): Result = {
        v.toValidated.toValidatedNel[B, A].toResult
      }

      override def toString = "Operation[Xor]"
    }

  implicit def resultOperation = new Operation[Result] {
    def result(r: Result) = r

    override def toString = "Operation[Result]"
  }

  implicit def tryOperation[A: Operation] = new Operation[Try[A]] {
    def result(t: Try[A]) = {
      t match {
        case util.Success(a) => a.toResult
        case util.Failure(e) => LogFatal("evaluating try", e).publish.fail
      }
    }

    override def toString = "Operation[Try]"
  }

  implicit def disjunctionOperation[A: ParcelOperation, B: ParcelOperation] =
    new Operation[A \/ B] {
      def result(a: A \/ B) = {
        a match {
          case -\/(e) => e.toParcel.fail
          case \/-(r) => r.toResult
        }
      }

      override def toString = "Operation[\\/]"
    }

  implicit def effectOperation =
    new Operation[Effect] {
      def result(eff: Effect) = {
        FlatMapEffect(eff, "by operation").internal.success
      }

      override def toString = "Operation[Effect]"
  }
}

object Operation
extends OperationInstances
{
  implicit lazy val parcelOperation = new ParcelOperation[Parcel] {
    def parcel(prc: Parcel) = prc

    override def toString = "Operation[Parcel]"
  }

  def message[A, B <: Message](f: A => B) = new Operation[A] {
    def result(a: A) = f(a).publish.success
  }
}
