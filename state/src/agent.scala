package tryp
package droid
package state

import shapeless._
import shapeless.tag.@@

import scalaz._, Scalaz._, concurrent.Strategy, stream.async

trait LogMachine
extends SimpleMachine
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

trait To

trait Agent
extends Logging
with StateStrategy
{
  def cachedPool = Agent

  def mediator: Mediator

  implicit protected lazy val toMachines: MessageTopic @@ To =
    tag.apply[To](MessageTopic())

  private[this] lazy val fromMachines = MessageTopic()

  def machines: List[Machine] = Nil

  def allMachines[A](f: Machine ⇒ A) = machines map (f)

  def send(msg: Message) = sendAll(msg.wrapNel)

  def sendAll(msgs: NonEmptyList[Message]) = {
    messageIn.enqueueAll(msgs.toList) !? s"enqueue messages $msgs in $this"
  }

  val ! = send _

  lazy val messageOut = machines foldMap(_.run())

  // FIXME remove fromMachines?
  // merge sending and receiving from machine as a channel?
  protected def runMachines() = {
    messageOut
      .to(fromMachines.publish)
      .infraRunAsync("publish messages from machines")
    mediator.subscribe
      .merge(messageIn.dequeue)
      .to(toMachines.publish)
      .infraRunAsync("publish mediator messages to machines")
    // messageOut
    fromMachines.subscribe
      .to(mediator.publish)
      .infraRunAsync("publish machine messages to mediator")
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

  lazy val logMachine = new LogMachine {}

  override def machines = logMachine :: super.machines
}
