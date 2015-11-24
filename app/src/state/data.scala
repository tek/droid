package tryp
package droid.state

import concurrent.duration._

import scalaz._, Scalaz._

import Process._

trait BasicState
case object Pristine extends BasicState
case object Initialized extends BasicState
case object Initializing extends BasicState

trait Message
{
  def toFail = this.failureNel[Message]
}

case class Create(args: Map[String, String], state: Option[Bundle])
extends Message
case object Resume extends Message
case object Update extends Message
case object Done extends Message
case object NopMessage extends Message
case class Toast(id: String) extends Message
case class ForkedResult(reason: String) extends Message
case object Debug extends Message
case class UiTask(ui: Ui[Result], timeout: Duration = 30 seconds)
extends Message
case class UiSuccessful(result: Any)
extends Message

trait InternalMessage
extends Message

case class SetInitialState(state: BasicState)
extends InternalMessage

trait Loggable extends Message
{
  def message: String
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
}

case class LogInfo(message: String)
extends Loggable

case class LogVerbose(message: String)
extends Loggable

case class LogDebug(message: String)
extends Loggable

case class UnknownResult[A: Show](result: A)
extends Loggable
{
  def message = result.show.toString
}

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

trait MessageInstances
{
  implicit val messageShow = new Show[Message] {
    override def show(msg: Message) = {
      msg match {
        case res @ UnknownResult(result) ⇒
          Cord(s"${res.className}(${res.message})")
        case _ ⇒
          msg.toString
      }
    }
  }

  implicit lazy val messageProcMonoid =
    Monoid.instance[Process[Task, Message]]((a, b) ⇒ a.merge(b), halt)
}

object Message
extends MessageInstances

trait Data
case object NoData extends Data
