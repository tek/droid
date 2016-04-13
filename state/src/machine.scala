package tryp
package droid
package state

import core._
import view.core._
import view._

import scalaz.stream
import stream.async

import scalaz.syntax.std.option._
import scalaz.syntax.apply._
import scalaz.syntax.show._

import cats._
import cats.std.all._

case class MachineTerminated(z: Machine)
extends Message

trait Machine
extends Logging
with StateStrategy
with cats.syntax.StreamingSyntax
with AnnotatedIO
{
  import Process._

  lazy val S = Zthulhu

  def stateAdmit: StateAdmission = PartialFunction.empty

  protected def initialZ = Zthulhu()

  protected def initialMessages: MProc = halt

  protected val internalMessageIn = async.unboundedQueue[Message]

  private[this] val forkedEffectResults = async.unboundedQueue[Parcel]

  private[this] val running = async.signalOf(false)

  private[this] val quit = async.signalOf(false)

  private[this] val term = async.signalOf(false)

  val current = async.signalUnset[Zthulhu]

  private[this] def idle: Process[Task, Boolean] = size map(_ == 0)

  protected def uncurriedTransitions(z: Zthulhu, m: Message) = {
    byMessage(z, m)
      .orElse(byState(z, m))
      .orElse(unmatched(m).lift(z))
  }

  protected def byMessage(z: Zthulhu, m: Message): Option[TransitResult] = {
    preselect(m)
      .orElse(unmatchedMessageType)
      .lift(m)
      .sideEffect(_ => log.debug(s"accepted $m"))
      .flatMap(_.lift(z))
      .sideEffect(_ => log.debug(s"transitioned by $m"))
  }

  protected def byState(z: Zthulhu, m: Message): Option[TransitResult] = {
    stateAdmit
      .lift(z)
      .flatMap(_.lift(m))
  }

  protected def preselect: Preselection = {
    case m: InternalMessage => internalMessage
    case m: Message => internalAdmit
  }

  protected def internalAdmit: Admission = extraAdmit orElse admit

  def admit: Admission

  def extraAdmit: Admission = PartialFunction.empty

  protected def interrupt = {
    term.discrete
      .merge(idle.when(quit.discrete))
  }

  private[this] def fsmProc(input: MProc, initial: Zthulhu): MProc = {
    val in = Nel(internalMessageIn.dequeue, input, initialMessages)
      .reduce
      .sideEffect(m => log.trace(s"processing ${m.show}"))
    interrupt
      .wye(in)(stream.wye.interrupt)
      .fsm(initial, uncurriedTransitions)
      .forkW(current.pipeIn)
      .merge(forkedEffectResults.dequeue)
      .forkTo(internalMessageIn.enqueue) { case Internal(m) => m }
  }

  def debugStates = false

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

  def isRunning = {
    running.continuous.take(1)
      .infraValueShortOr(false)(s"check whether $description is running")
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

  def handle: String = this.className.stripSuffix("Machine").snakeCase

  def machinePrefix: List[String] = List("mach")

  def machineName: Unit Xor String = ().left

  def info = s"$description ($waitingTasks waiting)"

  override def loggerName = {
    super.loggerName ::: machinePrefix ::: handle :: machineName.toList
  }

  final def internalMessage: Admission = {
    case SetInitialState(s) => setInitialState(s)
    case MachineStarted => machineStarted
    case QuitMachine => quitMachine
    case FlatMapEffect(eff, _) => flatMapEffect(eff)
    case Fork(eff, desc) => forkEffect(eff, desc)
    case Async(task, desc) => asyncTask(task, desc)
    case PublishMessage(msg) => publishMessage(msg)
    case prc: Parcel => { s => s << prc }
  }

  private[this] def setInitialState(s: BasicState): Transit = {
    case S(Pristine, d) => S(s, d)
  }

  private[this] def machineStarted: Transit =
      _ << setMachineRunning << initialEffects

  private[this] def setMachineRunning =
    running.setter(true).stateSideEffect("signal <running>")

  protected def initialEffects: Effect = halt

  private[this] def quitMachine: Transit =
    _ << (quit.set(true) *> finished.run)

  private[this] def flatMapEffect(eff: Effect): Transit = _ << eff

  def forkEffect(eff: Effect, desc: String): Transit = {
    case s =>
      Zthulhu.handleResult(eff)
        .stripW
        .to(forkedEffectResults.enqueue)
        .infraFork(s"fork effect: $desc")
      s
  }

  def asyncTask(task: Task[_], desc: String): Transit = {
    case s =>
      task.unsafePerformSyncAttempt match {
        case scalaz.\/-(result) =>
          log.debug(s"async task '$desc' succeeded: $result")
        case scalaz.-\/(err) =>
          log.error(s"async task '$desc' failed: $err")
      }
      s
  }

  def publishMessage(msg: Message): Transit = _ << msg.publish

  def unmatched(m: Message): Transit = {
    case s if debugStates =>
      log.trace(s"unmatched message at $s: $m")
      s
  }

  lazy val unmatchedMessageType: Admission = {
    case m if debugStates => {
      case s =>
        log.trace(s"unmatched message type at $s: $m")
        s
    }
  }

  protected def broadcast(msg: Message) = {
    send(PublishMessage(msg))
  }

  def instance_PublishFilter_IOTask[F[_, _]: PerformIO, A: Operation, C]
  : PublishFilter[IOTask[F, A, C]] = new PublishFilter[IOTask[F, A, C]] {
    def allowed = true
  }

  def instance_PublishFilter_IOFun[A, C]
  : PublishFilter[IOFun[A, C]] = new PublishFilter[IOFun[A, C]] {
    def allowed = true
  }
}
