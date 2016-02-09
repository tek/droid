package tryp
package droid
package state

import concurrent.duration.FiniteDuration

import shapeless._
import shapeless.tag.@@

import scalaz.concurrent.Strategy, scalaz.stream
import stream._
import Process._
import ScalazGlobals._

import cats._
import cats.data._
import cats.syntax.all._

trait LogMachine
extends Machine
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
{
  case class AddSub(subs: Nes[Agent])
  extends Message

  case class AgentData(sub: List[Agent])
  extends Data
}
import Agent._

trait To

trait Agent
extends Machine
{
  def sub: Streaming[Agent] = Streaming.Empty()

  type TMT = MessageTopic @@ To

  private[this] val dynamicSubAgentsIn = async.unboundedQueue[Agent]

  protected val publishDownIn = tag[To](MessageTopic())

  private[this] val publishLocalIn = tag[To](MessageTopic())

  override protected val initialZ = Zthulhu(data = AgentData(Nil))

  def machines: Streaming[Machine] = Streaming.Empty()

  def allMachines[A](f: Machine ⇒ A) = machines map(f)

  val ! = send _

  protected def publishTo(to: TMT, dest: String, msgs: MNes) = {
    emitAll(msgs.toList).to(to.publish) !?
      s"enqueue messages $msgs to $dest in $this"
  }

  def publishAll(msgs: MNes) = {
    publishTo(publishDownIn, "subagents", msgs)
  }

  def publishOne(msg: Message) = {
    publishAll(Nes(msg))
  }

  def publishLocalAll(msgs: MNes) = {
    publishTo(publishLocalIn, "machines", msgs)
  }

  def publishLocalOne(msg: Message) = {
    publishLocalAll(Nes(msg))
  }

  def machinesMessageOut(in: MProc) =
    machines foldMap(_.runWithInput(in))

  def agentMain(in: MProc): MProc = {
    val down = publishDownIn.subscribe.merge(in)
    val local = down.merge(publishLocalIn.subscribe)
    val mainstream =
      machinesMessageOut(local)
        .merge(fixedSubAgents(down))
        .merge(dynamicSubAgents(down))
        .merge(run)
    interrupt.wye(mainstream)(stream.wye.interrupt)
  }

  def fixedSubAgents(in: MProc) = {
    sub.foldMap(_.agentMain(in))
  }

  private[this] def dynamicSubAgents(in: MProc) = {
    nondeterminism.njoin(0, 0)(dynamicSubAgentsRunner(in))
  }

  private[this] def dynamicSubAgentsRunner(in: MProc) = {
    dynamicSubAgentsIn.dequeue.map(_.agentMain(in))
  }

  def addSubAgent(sub: Agent) = {
    send(AddSub(Nes(sub)))
  }

  /**
   * 1. Obtain the current sub agent count; if not available instantaneously,
   * use 0.
   * 2. Initiate the sub agent adding procedure by sending AddSub
   * 3. Wait for the adding procedure to finish for at most @timeout:
   *  a) Create a process that sleeps for @timeout, then emits false 
   *  b) Create a process that compares the count obtained in 1. with the
   *  current sub agent count
   *  c) Create a process that emits true when process b) becomes true
   *  d) Merge processes a) and c) and take the first emitted value
   * This returns true if the procedure successfully waited until the agent
   * was added properly, false if the @timeout threshold was hit before that.
   */
  def startSubAgent(sub: Agent, timeout: FiniteDuration)
  : Process[Task, Boolean] = {
    subAgentCountP
      .headOr(0)
      .either(sendP(AddSub(Nes(sub))))
      .stripO
      .flatMap { before ⇒
        emit(true).when(subAgentCountP.map(_ >= before + 1))
      }
      .timedHeadOr(timeout, false)
  }

  def admit: Admission = {
    case AddSub(subs) ⇒ addSub(subs)
  }

  private[this] def addSub(add: Nes[Agent]): Transit = {
    case S(s, AgentData(sub)) ⇒
      log.debug(s"Adding sub agents $add")
      S(s, AgentData(sub ::: add.toList)) <<
        emitAll(add.toList).to(dynamicSubAgentsIn.enqueue)
          .effect(s"enqueue sub agents in $description")
  }

  def subAgentCountP = {
    current.discrete
      .map {
        case S(_, AgentData(sub)) ⇒
          sub.length
      }
  }

  def waitForSubAgentCount(num: Int, timeout: Duration) = {
    halt.when(subAgentCountP.map(_ >= num))
      .infraRunFor(s"wait for sub agent count $num in $this", timeout)
  }

  protected def postRunAgent(): Unit = ()

  def killMachines() = {
    allMachines(_.kill())
  }

  def joinMachines() = {
    allMachines(_.join())
  }

  def waitMachines() = {
    allMachines(_.waitIdle())
  }

  lazy val fallbackRootAgent = new RootAgent {
    def handle = "fallbackMediator"
  }
}

trait RootAgent
extends Agent
{
  lazy val logMachine = new LogMachine {}

  override def machines = logMachine %:: super.machines

  def rootAgent = {
    agentMain(halt)
      .to(publishDownIn.publish)
  }

  protected def forkAgent() = {
    rootAgent
      .infraFork(s"root agent $this")
  }

  def runAgent() = {
    forkAgent()
    postRunAgent()
  }
}
