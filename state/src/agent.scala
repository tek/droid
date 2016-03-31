package tryp
package droid
package state

import concurrent.duration.FiniteDuration

import shapeless._
import shapeless.tag.@@

import scalaz.stream.{async, wye, merge, process1}
import Process._
import ScalazGlobals._

import cats._
import cats.data._
import cats.syntax.all._

import droid.state.core._

trait LogMachine
extends Machine
{
  def handle = "log"

  def logError(msg: String): Transit = {
    case s =>
      log.error(msg)
      s
  }

  def logInfo(msg: String): Transit = {
    case s =>
      log.info(msg)
      s
  }

  val admit: Admission = {
    case m: LogError => logError(m.message)
    case m: LogFatal => logError(m.message)
    case m: LogInfo => logInfo(m.message)
    case UnknownResult(msg) => logInfo(msg.toString)
    case m: EffectSuccessful => logInfo(m.message)
    case m: Loggable => logInfo(m.toString)
  }
}

case class MessageTopic(topic: Topic[Message] = async.topic[Message]())
extends AnyVal
{
  def subscribe = topic.subscribe
  def publish = topic.publish
}

object AgentStateData
{
  case class AddSub(subs: Nes[Agent])
  extends Message

  case class StopSub(subs: Nes[Agent])
  extends Message

  case object SubAdded
  extends Message

  case class AgentData(sub: List[Agent])
  extends core.Data
}
import AgentStateData._

trait To

@Publish(SubAdded)
trait Agent
extends Machine
{
  def sub: Streaming[Agent] = Streaming.Empty()

  type TMT = Topic[Message] @@ To

  def mkTopic = async.topic[Message]()(strat)

  private[this] lazy val dynamicSubAgentsIn = async.unboundedQueue[Agent]

  private[this] lazy val scheduledMessages = async.unboundedQueue[Message]

  protected lazy val publishDownIn = tag[To](mkTopic)

  private[this] lazy val publishLocalIn = tag[To](mkTopic)

  override protected lazy val initialZ = Zthulhu(data = AgentData(Nil))

  def machines: Streaming[Machine] = Streaming.Empty()

  def allMachines[A](f: Machine => A) = machines map(f)

  val ! = send _

  override def machinePrefix = super.machinePrefix :+ "ag"

  protected def publishToP(to: TMT, msgs: MNes) = {
    emitAll(msgs.toList).to(to.publish)
  }

  protected def publishTo(to: TMT, dest: String, msgs: MNes) = {
    publishToP(to, msgs) !?
      s"enqueue messages $msgs to $dest in $this"
  }

  def publish(head: Message, tail: Message*) = {
    publishToP(publishDownIn, Nes(head, tail: _*))
  }

  def publishAll(msgs: MNes) = {
    publishTo(publishDownIn, "subagents", msgs)
  }

  def publishOne(msg: Message) = {
    publishAll(Nes(msg))
  }

  def publishLocal(msgs: MNes) = {
    publishToP(publishLocalIn, msgs)
  }

  def publishLocalAll(msgs: MNes) = {
    publishTo(publishLocalIn, "machines", msgs)
  }

  def publishLocalOne(msg: Message) = {
    publishLocalAll(Nes(msg))
  }

  def scheduleAll(msgs: MNes) = {
    if (isRunning) publishAll(msgs)
    else {
      scheduledMessages
        .enqueueAll(msgs.toList) !? s"schedule messages $msgs in $description"
    }
  }

  def scheduleOne(msg: Message) = {
    scheduleAll(Nes(msg))
  }

  def machinesMessageOut(in: MProc) =
    machines foldMap(_.runWithInput(in))

  // def machinesMessageOut(in: MProc) =
  //   (this %:: machines) foldMap(_.runWithInput(in))

  def agentMain(in: MProc): MProc = {
    val down = publishDownIn.subscribe
      // .sideEffect(a => log.debug(s"pdi: $a"))
      .merge(in)
    val local = publishLocalIn.subscribe
      // .sideEffect(a => log.debug(s"pli: $a"))
      .merge(down)
    val mainstream =
      machinesMessageOut(local)
        .merge(fixedSubAgents(down))
        .merge(dynamicSubAgents(down))
        .merge(runWithInput(down))
        .forkTo(publishLocalIn.publish) {
          case ToLocal(m) =>
            p(s"$description found ToLocal: $m")
            m
        }
        .forkTo(internalMessageIn.enqueue) {
          case ToAgent(m) =>
            p(s"$description found ToAgent: $m")
            m
        }
        .forkTo(publishDownIn.publish) {
          case ToSub(m) =>
            p(s"$description found ToSub: $m")
            m
        }
    interrupt.wye(mainstream)(wye.interrupt)
  }

  def fixedSubAgents(in: MProc) = {
    sub.foldMap(_.agentMain(in))
  }

  private[this] def dynamicSubAgents(in: MProc) = {
    merge.mergeN(dynamicSubAgentsRunner(in))
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
      .flatMap { before =>
        emit(true).when(subAgentCountP.map(_ >= before + 1))
      }
      .timedHeadOr(timeout, false)
  }

  def admit: Admission = {
    case AddSub(subs) => addSub(subs)
    case StopSub(subs) => stopSub(subs)
  }

  private[this] def addSub(add: Nes[Agent]): Transit = {
    case S(s, AgentData(sub)) =>
      log.debug(s"adding sub agents $add")
      S(s, AgentData(sub ::: add.toList)) <<
        emitAll(add.toList).to(dynamicSubAgentsIn.enqueue)
          .stateSideEffect(s"enqueue sub agents in $description") <<
            SubAdded
  }

  private[this] def stopSub(agents: Nes[Agent]): Transit = {
    case S(s, AgentData(sub)) =>
      agents map(_.join())
      S(s, AgentData(sub filter agents.contains))
  }

  def subAgentCountP = {
    current.discrete
      .map {
        case S(_, AgentData(sub)) =>
          sub.length
      }
  }

  def waitForSubAgentCount(num: Int, timeout: Duration) = {
    halt.when(subAgentCountP.map(_ >= num))
      .infraRunFor(s"wait for sub agent count $num in $this", timeout)
  }

  /** dequeueAvailable blocks until at least one element is queued.
   * Therefore, dequeue nondeterministically and remove the dummy values,
   * which results in a discrete Process that may or may not contain a Seq.
   * If this isn't done like this, `headOr` will block if the queue is empty.
   */
  override protected def initialEffects: Effect = {
    val messages = scheduledMessages.dequeueAvailable.availableOrHalt
    (messages.headOr(Nil) |> process1.unchunk)
      .map {
        case prc: Parcel => prc
        case m => (m: Parcel)
      }
      .stateEffect
  }

  def killMachines() = {
    allMachines(_.kill())
  }

  def joinMachines() = {
    allMachines(_.join())
  }

  def waitMachines() = {
    allMachines(_.waitIdle())
  }
}

trait RootAgent
extends Agent
{
  // lazy val logMachine = new LogMachine {}

  // override def machines = logMachine %:: super.machines

  def rootAgent = {
    agentMain(halt)
      .collect {
        case ToRoot(m) =>
          // p(s"$description found ToRoot: $m")
          m
      }
      .sideEffect(m => log.debug(s"publishing $m"))
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

  protected def postRunAgent(): Unit = ()
}
