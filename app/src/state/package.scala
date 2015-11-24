package tryp
package droid

import scalaz._, Scalaz._, stream._, async._

package object state
extends droid.meta.GlobalsBase
with droid.meta.Forward
with droid.meta.AndroidTypes
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

  type Result = ValidationNel[Message, Message]

  type Effect = Process[Task, Result]

  type ViewTransitionResult = (Zthulhu, Effect)

  type ViewTransition = PartialFunction[Zthulhu, ViewTransitionResult]

  type ViewTransitions = PartialFunction[Message, ViewTransition]

  type TransitionsSelection = Message ⇒ ViewTransitions
}
