package tryp
package droid

import scalaz.{stream, concurrent}, stream._, async._
import scalaz.ValidationNel

import cats._
import cats.data._
import cats.syntax.all._

package object state
extends droid.core.meta.Globals
with droid.core.meta.AndroidTypes
with tryp.slick.sync.meta.Globals
with ToStateEffectSyntax
with MiscEffectOps
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

  type Result = ValidationNel[Parcel, Parcel]

  type Effect = Process[Task, Result]

  type TransitResult = (Zthulhu, Effect)

  type Transit = PartialFunction[Zthulhu, TransitResult]

  type Admission = PartialFunction[Message, Transit]

  type Preselection = Message ⇒ Admission

  type Nes[A] = OneAnd[Streaming, A]
  type MNes = Nes[Message]

  def MNes(m: Message, t: Message*) =
    OneAnd[Streaming, Message](m, Streaming.fromList(t.toList))
}
