package tryp
package droid
package state

import shapeless._

import scalaz._, Scalaz._, concurrent.Strategy, stream.async

trait LogMachine
extends Machine[HNil]
{
  def handle = "log"

  def logError(msg: String): Transit = {
    case s ⇒
      log.error(msg)
      s
  }

  def logInfo(msg: String): Transit = {
    case s ⇒
      log.info(msg)
      s
  }

  val admit: Admission = {
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

object Agent
extends CachedPool

trait Agent
extends Logging
with CachedStrategy
{
  def cachedPool = Agent

  def mediator: Mediator

  implicit protected lazy val toMachines = MessageTopic()

  private[this] lazy val fromMachines = MessageTopic()

  lazy val logMachine = new LogMachine {}

  def machines: List[Machine[_]] = logMachine :: Nil

  def allMachines[A](f: Machine[_] ⇒ A) = machines map(f)

  def send(msg: Message) = sendAll(msg.wrapNel)

  def sendAll(msgs: NonEmptyList[Message]) = {
    messageIn.enqueueAll(msgs.toList) !? s"enqueue messages $msgs in $this"
  }

  val ! = send _

  lazy val messageOut = machines foldMap(_.run())

  protected def runMachines() = {
    messageOut
      .to(fromMachines.publish)
      .infraRunAsync("machine emission")
    mediator.subscribe
      .merge(messageIn.dequeue)
      .logged("agent to machines")
      .to(toMachines.publish)
      .infraRunAsync("machine reception")
    fromMachines.subscribe
      .to(mediator.publish)
      .infraRunAsync("machine to mediator")
  }

  def initMachines() = {
    runMachines()
    postRunMachines()
  }

  protected def postRunMachines(): Unit = ()

  lazy val messageIn = async.unboundedQueue[Message]

  def killMachines() = {
    allMachines(_.kill())
  }

  def joinMachines() = {
    allMachines(_.join())
  }

  lazy val fallbackMediator = new Mediator {}
}

trait SolitaryAgent
extends Agent
{
  lazy val mediator = fallbackMediator
}

trait Mediator
extends Agent
{
  private[this] val exchange = MessageTopic()

  def publish = exchange.publish

  def subscribe = exchange.subscribe

  def mediator = this
}
