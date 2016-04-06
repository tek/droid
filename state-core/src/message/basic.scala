package tryp
package droid
package state
package core

import scalaz._, Scalaz._

import Process._

case class Create(args: Params, state: Option[Bundle])
extends Message

case object Resume
extends Message

case object Update
extends Message

case object Done
extends Message

case object NopMessage
extends Message

case class Toast(id: String)
extends Message

case class ForkedResult(reason: String)
extends Message

case object Debug
extends Message

case object UnitTask
extends Message

// case class UiTask(ui: Ui[Result], timeout: Duration = 30 seconds)
// extends Message

case class UiSuccessful(result: Any)
extends Message

case class IOVSuccessful(result: Any)
extends Message

case object MachineStarted
extends InternalMessage

case class SetInitialState(state: BasicState)
extends InternalMessage

case object QuitMachine
extends InternalMessage

case class FlatMapEffect(eff: Effect, desc: String)
extends InternalMessage
{
  override def toString = s"FlatMap($desc)"
}

case class Fork(eff: Effect, message: String)
extends InternalMessage
with Loggable
{
  override def toString = s"Fork($message)"
}

case class Async(task: Task[_], message: String)
extends InternalMessage
{
  override def toString = s"Async($message)"
}

case class LogError(description: String, msg: String)
extends Loggable
{
  lazy val message = s"error while $description: $msg"
}

case class LogFatal(description: String, error: Throwable)
extends Loggable
{
  lazy val message = Error.withTrace(s"exception while $description", error)

  override def toString = s"LogFatal($description)"
}

case class LogInfo(message: String)
extends Loggable

case class LogVerbose(message: String)
extends Loggable

case class LogDebug(message: String)
extends Loggable

case class EffectSuccessful(description: String, result: Any = Unit)
extends Loggable
{
  lazy val message = {
    val res = result match {
      case Unit => ""
      case _ => s" ($result)"
    }
    s"successful effect: $description$res"
  }
}

case class PublishMessage(message: Message)
extends InternalMessage
