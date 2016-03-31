package tryp
package droid
package state
package core

import scalaz.{stream, concurrent}, stream._, async._
import scalaz.ValidationNel

import cats._
import cats.data._
import cats.syntax.all._

trait Decls
{
  type Result = ValidationNel[Parcel, Parcel]
  type Effect = Process[Task, Result]
  type TransitResult = (Zthulhu, Effect)
  type Transit = PartialFunction[Zthulhu, TransitResult]
  type TransitF = Zthulhu => TransitResult
  type Admission = PartialFunction[Message, Transit]
  type StateTransit = PartialFunction[Message, TransitResult]
  type StateAdmission = PartialFunction[Zthulhu, StateTransit]
  type Preselection = Message => Admission
  type MProc = Process[Task, Message]
  type Nes[A] = OneAnd[Streaming, A]
  type MNes = Nes[Message]

  def Nes[A](m: A, t: A*) =
    OneAnd[Streaming, A](m, Streaming.fromList(t.toList))

  implicit def partializeTransitF(f: TransitF): Transit = {
    case s => f(s)
  }
}

trait StateDecls
{
  private[state] type Signal[A] = mutable.Signal[A]
  private[state] type Queue[A] = mutable.Queue[A]
  private[state] type Topic[A] = mutable.Topic[A]
  private[state] type Process[F[_], A] = stream.Process[F, A]
  private[state] type Process1[I, O] = stream.Process1[I, O]
  private[state] type Task[A] = concurrent.Task[A]
  private[state] type Writer1[W, I, O] = stream.Writer1[W, I, O]
  private[state] val Process = stream.Process
  private[state] val Signal = mutable.Signal
  private[state] val Task = concurrent.Task
  private[state] val writer = stream.writer
}

trait ExportDecls
extends Decls
{
  type Zthulhu = core.Zthulhu
  val Zthulhu = core.Zthulhu
}

trait Exports
extends droid.core.Exports
with ToOperationSyntax
with ToZthulhuCtor

object meta
{
  trait Globals
  extends droid.core.Exports
  with droid.core.ExportDecls
  with tryp.slick.sync.meta.Globals
  with Decls
  with StateDecls
}

object `package`
extends meta.Globals
