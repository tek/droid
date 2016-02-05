package tryp
package droid
package state

import shapeless._
import shapeless.tag.@@

import scalaz.concurrent.Strategy, scalaz.stream.async

import cats._
import cats.data._
import cats.syntax.all._

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
{
  case class AddSub(subs: Nes[Agent])
  extends Message
}
import Agent._

trait To

trait Agent
extends Machine
{
  def mediator: Mediator

  def sub: Streaming[Agent] = Streaming.Empty()

  implicit protected lazy val toMachines: MessageTopic @@ To =
    tag.apply[To](MessageTopic())

  def messageTopic = tag.apply[To](MessageTopic())

  def machines: Streaming[Machine] = Streaming.Empty()

  def allMachines[A](f: Machine ⇒ A) = machines map(f)

  val ! = send _

  lazy val messageOut = machines foldMap(_.run())

  def machinesComm: Process[Task, Message] = {
    mediator.subscribe
      .merge(internalMessageIn.dequeue)
      .either(messageOut)
      .observeW(toMachines.publish)
      .stripW
      .merge(subMachinesComm)
  }

  def subMachinesComm = {
    sub.foldMap(_.machinesComm)
  }

  def isolatedMachinesComm = {
    machinesComm
      .to(mediator.publish)
  }

  protected def connectMachines() = {
    isolatedMachinesComm
      .infraFork("exchange with mediator")
  }

  def admit: Admission = {
    case AddSub(subs) ⇒ addSub(subs)
  }

  def addSub(subs: Nes[Agent]): Transit = {
    case s ⇒ s
  }

  def integrate = {

  }

  def initMachines() = {
    connectMachines()
    postRunMachines()
  }

  protected def postRunMachines(): Unit = ()

  def killMachines() = {
    allMachines(_.kill())
  }

  def joinMachines() = {
    allMachines(_.join())
  }

  def waitMachines() = {
    allMachines(_.waitIdle())
  }

  lazy val fallbackMediator = new Mediator {
    def handle = "fallbackMediator"
  }
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

  override def machines = logMachine %:: super.machines
}
