package tryp
package droid
package state
package core

import cats._
import cats.syntax.all._

@exportNames(Zthulhu, Parcel, Message, Data, NoData, StateStrategy)
trait Names
extends droid.core.Names

@export
trait Decls
{
  import scalaz.stream._

  type Result = cats.data.ValidatedNel[Parcel, Parcel]
  type Effect = scalaz.stream.Process[scalaz.concurrent.Task, Result]
  type TransitResult = (Zthulhu, Effect)
  type Transit = PartialFunction[Zthulhu, TransitResult]
  type TransitF = Zthulhu => TransitResult
  type Admission = PartialFunction[Message, Transit]
  type StateTransit = PartialFunction[Message, TransitResult]
  type StateAdmission = PartialFunction[Zthulhu, StateTransit]
  type Preselection = Message => Admission
  type MProc = scalaz.stream.Process[scalaz.concurrent.Task, Message]
  type MNes = Nes[Message]

  implicit def partializeTransitF(f: TransitF): Transit = a => a match {
    case s => f(s)
  }
}

import scalaz.stream, stream.async

@exportNames(async.mutable.Signal, async.mutable.Queue, async.mutable.Topic,
  stream.Process1, scalaz.concurrent.Task, stream.writer,
  stream.Writer1)
trait StateDecls
{
  import scalaz.stream, stream.async

  type Process[F[_], A] = stream.Process[F, A]
  val Process = stream.Process
}

@export
trait Exports
extends Names
with Decls
with droid.core.Exports

trait All
extends droid.core.All
with ToOperationSyntax
with ToZthulhuCtor

@integrate(droid.core)
object `package`
extends tryp.slick.sync.meta.Globals
with Decls
with StateDecls
