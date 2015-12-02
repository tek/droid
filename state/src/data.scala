package tryp
package droid.state

import scalaz._, Scalaz._

import Process._

trait BasicState
case object Pristine extends BasicState
case object Initialized extends BasicState
case object Initializing extends BasicState

case class MachineTerminated(
  z: Machine[_ <: shapeless.HList, _ <: shapeless.HList])
extends Message

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

case class UiTask(ui: Ui[Result], timeout: Duration = 30 seconds)
extends Message

case class UiSuccessful(result: Any)
extends Message

trait InternalMessage
extends Message

case class SetInitialState(state: BasicState)
extends InternalMessage

case class LogError(description: String, msg: String)
extends Loggable
{
  lazy val message = s"error while $description: $msg"
}

case class LogFatal(description: String, error: Throwable)
extends Loggable
{
  lazy val message = Error.withTrace(s"exception while $description", error)
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
      case Unit ⇒ ""
      case _ ⇒ s" ($result)"
    }
    s"successful effect: $description$result"
  }
}

trait Data
case object NoData extends Data