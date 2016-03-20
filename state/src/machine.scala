package tryp
package droid
package state

import shapeless._
import shapeless.tag.@@

import scalaz.syntax.std.option._

import scalaz.stream
import stream.async
import Process._
import scalaz.syntax.apply._
import scalaz.syntax.show._

import cats._
import cats.data._
import cats.syntax.all._
import cats.std.all._

import Zthulhu._

trait Machine
extends Logging
with StateStrategy
with cats.syntax.StreamingSyntax
{
  val S = Zthulhu

  def admit: Admission

  def stateAdmit: StateAdmission = PartialFunction.empty

  protected def initialZ = Zthulhu()

  protected def initialMessages: MProc = halt

  private[this] val internalMessageIn = async.unboundedQueue[Message]

  private[this] val forkedEffectResults = async.unboundedQueue[Parcel]

  private[this] val running = async.signalOf(false)

  private[this] val quit = async.signalOf(false)

  private[this] val term = async.signalOf(false)

  val current = async.signalUnset[Zthulhu]

  private[this] def idle: Process[Task, Boolean] = size map(_ == 0)

  protected def preselect: Preselection = {
    case m: InternalMessage => internalMessage
    case m: Message => admit
  }

  protected def uncurriedTransitions(z: Zthulhu, m: Message) = {
    byMessage(z, m)
      .orElse(byState(z, m))
      .orElse(unmatched(m).lift(z))
      .toMaybe
  }

  protected def byMessage(z: Zthulhu, m: Message): Option[TransitResult] = {
    preselect(m)
      .orElse(unmatchedMessageType)
      .lift(m)
      .flatMap(_.lift(z))
  }

  protected def byState(z: Zthulhu, m: Message): Option[TransitResult] = {
    stateAdmit
      .lift(z)
      .flatMap(_.lift(m))
  }

  protected def interrupt = {
    term.discrete
      .merge(idle.when(quit.discrete))
  }

  private[this] def fsmProc(input: MProc, initial: Zthulhu): MProc = {
    val in = Nel(internalMessageIn.dequeue, input, initialMessages)
      .reduce
      .sideEffect(m => log.debug(s"processing ${m.show}"))
    interrupt
      .wye(in)(stream.wye.interrupt)
      .fsm(initial, uncurriedTransitions)
      .forkW(current.pipeIn)
      .merge(forkedEffectResults.dequeue)
      .separateMap(_.publish)(_.message)
      .forkW(internalMessageIn.enqueue)
  }

  val debugStates = false

  def run = runWith(halt, initialZ)

  def runWithInput(input: MProc) = runWith(input, initialZ)

  def runWithInitial(initial: Zthulhu) = runWith(halt, initial)

  def runWith(input: MProc, initial: Zthulhu) = {
    if (debugStates) {
      current.discrete
        .map(z => log.debug(z.toString))
        .infraFork("debug state printer")
    }
    fsmProc(emit(MachineStarted) ++ input, initial)
      .onComplete { running.setter(false).flatMap(_ => halt) }
  }

  def fork(initial: Zthulhu) = {
    runWithInitial(initial).infraFork(s"autonomous $this")
  }

  def sendP(msg: Message) = emit(msg).to(internalMessageIn.enqueue)

  def send(msg: Message) = {
    sendAll(Nes(msg))
  }

  def sendAll(msgs: MNes) = {
    internalMessageIn
      .enqueueAll(msgs.toList) !? s"enqueue messages $msgs in $description"
  }

  // kill the state machine
  // the 'term' signal is woven into the main process using the wye combinator
  // 'interrupt', which listens asynchronously and terminates instantly
  def kill() = {
    term.set(true) !? "set signal term"
    internalMessageIn.kill !? "kill internal message queue"
  }

  // gracefully shut down the state maching
  // the 'quit' signal uses the deterministic tee combinator 'until', which is
  // only emitted when the 'idle' signal is true
  // 'idle' is set when no messages are queued
  // *> combines the two Task instances via Apply.apply2
  // roughly equivalent to a flatMap(_ => b)
  def join() = {
    log.trace(s"terminating $this")
    (quit.set(true) *> finished.run)
      .infraRunFor("wait for finished signal", 20 seconds)
  }

  def waitForRunning(timeout: Duration = 20 seconds) = {
    running.discrete.exists(a => a)
      .infraRunFor(s"wait for $this to run", timeout)
  }

  def waitIdle(timeout: Duration = 20 seconds) = {
    log.trace(s"waiting for $this to idle ($waitingTasks)")
    idle.exists(a => a)
      .infraRunFor(s"wait for $this to idle", timeout)
  }

  def finished = running.continuous.exists(!_)

  private[this] def size = {
    internalMessageIn.size.discrete
  }

  private[this] def waitingTasks = {
    size.headOr(0).infraValueShortOr(0)(s"waiting tasks in $description")
  }

  def description = s"$handle state"

  override def toString = description

  def handle: String

  def machinePrefix: List[String] = List("mach")

  def machineName: Unit Xor String = ().left

  def info = s"$description ($waitingTasks waiting)"

  override def loggerName = {
    super.loggerName ::: machinePrefix ::: handle :: machineName.toList
  }

  lazy val internalMessage: Admission = {
    case SetInitialState(s) => setInitialState(s)
    case MachineStarted => setMachineRunning
    case QuitMachine => quitMachine
    case FlatMapEffect(eff) => flatMapEffect(eff)
    case Fork(eff, desc) => forkEffect(eff, desc)
  }

  private[this] def setInitialState(s: BasicState): Transit = {
    case S(Pristine, d) => S(s, d)
  }

  private[this] def setMachineRunning: Transit = {
    case s =>
      s << running.setter(true).effect("set running signal to true")
  }

  private[this] def quitMachine: Transit = {
    case s =>
      s << (quit.set(true) *> finished.run)
  }

  private[this] def flatMapEffect(eff: Effect): Transit = {
    case s =>
      s << eff
  }

  def forkEffect(eff: Effect, desc: String): Transit = {
    case s =>
      Zthulhu.handleResult(eff)
        .stripW
        .to(forkedEffectResults.enqueue)
        .infraFork(s"fork effect $desc")
      s
  }

  def unmatched(m: Message): Transit = {
    case s if debugStates =>
      log.debug(s"unmatched message at $s: $m")
      s
  }

  lazy val unmatchedMessageType: Admission = {
    case m if debugStates => {
      case s =>
        log.debug(s"unmatched message type at $s: $m")
        s
    }
  }

  implicit def defaultPublishFilter[A <: Message] = new PublishFilter[A] {
    def allowed = false
  }
}
