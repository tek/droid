package tryp
package droid
package state

import scalaz._, Scalaz._, concurrent.Strategy, stream.async

trait LogMachine
extends Machine[HNil]
{
  def handle = "log"

  def logError(msg: String): ViewTransition = {
    case s ⇒
      log.error(msg)
      s
  }

  def logInfo(msg: String): ViewTransition = {
    case s ⇒
      log.info(msg)
      s
  }

  val transitions: ViewTransitions = {
    case m: LogError ⇒ logError(m.message)
    case m: LogFatal ⇒ logError(m.message)
    case m: LogInfo ⇒ logInfo(m.message)
    case UnknownResult(msg) ⇒ logInfo(msg.toString)
    case m: EffectSuccessful ⇒ logInfo(m.message)
    case m: Loggable ⇒ logInfo(m.toString)
  }
}

case class MessageTopic(topic: Topic[Message] = async.topic[Message]())
extends AnyVal
{
  def subscribe = topic.subscribe
  def publish = topic.publish
}

trait Agent
extends Logging
{
  implicit lazy val strat: Strategy = Strategy.Naive

  implicit lazy val messageTopic = MessageTopic()

  lazy val logMachine = new LogMachine {}

  def machines: List[Machine[_]] = logMachine :: Nil

  def allMachines[A](f: Machine[_] ⇒ A) = machines map(f)

  def send(msg: Message) = sendAll(msg.wrapNel)

  def sendAll(msgs: NonEmptyList[Message]) = {
    messageIn.enqueueAll(msgs.toList) !? s"enqueue messages $msgs in $this"
  }

  val ! = send _

  def runState() = {
    runPublisher()
    allMachines(_.runFsm())
  }

  protected def initState() = {
    runState()
    postRunState()
  }

  protected def postRunState(): Unit = ()

  lazy val messageIn = async.unboundedQueue[Message]

  lazy val messageOut = machines foldMap(_.messageOut.dequeue)

  private[this] def runPublisher() = {
    messageOut
      .merge(messageIn.dequeue)
      .to(messageTopic.publish)
      .run
      .infraRunAsync("message publisher")
  }

  def killState() = {
    allMachines(_.kill())
  }

  def joinState() = {
    allMachines(_.join())
  }
}
