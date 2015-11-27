package tryp
package droid
package state

import shapeless._

import concurrent.duration._

import shapeless.syntax.std.tuple._

import scalaz._, Scalaz._

import stream.async
import Process._

case class Zthulhu(state: BasicState = Pristine, data: Data = NoData)

object Zthulhu
{
  /* @param z current state machine
   * @param f (Z, M) ⇒ (Z, E), transforms a state and message into a new state
   * and a Process of effects of type E
   *
   * first, receive one item of type M representing a Message
   * calculate new state, may be empty, then the old state is used
   * the new state is emitted on the output side, the effects returned from
   * the transition function, if any, are emitted on the writer side and the
   * current state z is replaced by the return value of the transition function
   * in any case, the new or old state is used to recurse
   * error callbacks are invoked if the transition or effect throws an
   * exception and their results emitted
   */
  def stateLoop[Z, E, M, F[_]](z: Z)
  (f: (Z, M) ⇒ Maybe[(Z, Process[F, E])])
  (transError: (Z, M, Throwable) ⇒ E)
  (effectError: (Z, Z, M, Throwable) ⇒ E)
  : Writer1[Process[F, E], M, Z] = {
    receive1 { (a: M) ⇒
      val (state, effect) =
        Task(f(z, a)).attemptRun match {
          case \/-(Just((nz, e))) ⇒ nz.just → e
          case \/-(Empty()) ⇒ Maybe.empty[Z] → halt
          case -\/(t) ⇒ Maybe.empty[Z] → emit(transError(z, a, t))
        }
      val o = state.cata(emitO, halt)
      val w = effect.onFailure(t ⇒ Process(effectError(z, state | z, a, t)))
      val l = stateLoop(state | z)(f)(transError)(effectError)
      o ++ emitW(w) ++ l
    }
  }

  def fatalTransition(state: Zthulhu, cause: Message, t: Throwable) = {
    LogFatal(s"transitioning $state for $cause", t).toFail
  }

  def fatalEffect(state: Zthulhu, nextState: Zthulhu, cause: Message,
    t: Throwable) = {
    LogFatal(s"during io: $state ⇒ $nextState by $cause", t).toFail
  }

  // create a state machine from a source process emitting Message instances,
  // a transition function that turns messages and states into new states and
  // side effects which optionally produce new messages, and a handler process
  // that extract those new messages from the side effects.
  // returns a writer that emits the states on the output side and the new
  // messages on the write side.
  implicit class ProcessToZthulhu[F[_]](source: Process[F, Message])
  {
    type TS[F[_]] = (Zthulhu, Message) ⇒ Maybe[(Zthulhu, Process[F, Result])]

    def viewFsm(transition: TS[F], effect: Process1[Result, Message]) = {
      source
        .pipe(stateLoop(Zthulhu())(transition)(fatalTransition)(fatalEffect))
        .flatMapW(a ⇒ writer.liftW(a |> effect))
    }
  }

  implicit def amendZthulhuWithEmptyEffects
  (f: Zthulhu): TransitResult =
    (f, halt)

  implicit class TransitResultOps(r: TransitResult)
  {
    def <<[A: StateEffect](e: A): TransitResult = {
      (r.head, r.last ++ (e: Effect))
    }
  }

  implicit class ZthulhuOps(z: Zthulhu)
  {
    def <<[A: StateEffect](e: A) = (z: TransitResult) << e
  }
}

import Zthulhu._

abstract class Machine[A <: HList]
(implicit messageTopic: MessageTopic)
extends ToUiOps
with Logging
with FixedStrategy
{
  val threads = 5

  val S = Zthulhu

  def admit: Admission

  private[this] val internalMessageIn = async.unboundedQueue[Message]

  private[this] val externalMessageIn = async.unboundedQueue[Message]

  private[this] def messageIn = {
    internalMessageIn.dequeue.logged(s"$description internal")
      .merge(messageTopic.subscribe.logged(s"$description external"))
  }

  val messageOut = async.unboundedQueue[Message]

  private[this] val running = async.signalOf(true)

  private[this] val quit = async.signalOf(false)

  private[this] val term = async.signalOf(false)

  val current = async.signalUnset[Zthulhu]

  private[this] val idle: Process[Task, Boolean] =
    size map(_ == 0)

  private[this] lazy val unsafePerformIO: Process1[Result, Message] = {
    stream.process1
      .collect[Result, List[Message]] {
        case scalaz.Success(m) ⇒
          log.debug(s"task succeeded with ${m.show}")
          List(m)
        case scalaz.Failure(ms) ⇒
          log.debug(s"task failed with ${ms.show}")
          ms.toList
      }
      .flatMap(emitAll)
  }

  def dispatchResults(msgs: NonEmptyList[Message]) = {
    sendAll(msgs)
  }

  protected def preselect: Preselection = {
    case m: InternalMessage ⇒ internalMessage
    case m: Message ⇒ admit
  }

  protected def uncurriedTransitions(z: Zthulhu, m: Message) = {
    preselect(m)
      .orElse(unmatchedMessage)
      .lift(m)
      .getOrElse(unmatchedState(m))
      .lift(z)
      .toMaybe
  }

  private[this] lazy val fsmProc: Process[Task, Message] = {
    term.discrete
      .merge(idle.when(quit.discrete))
      .wye(messageIn)(stream.wye.interrupt)
      .viewFsm(uncurriedTransitions, unsafePerformIO)
      .observeO(current.pipeIn)
      .stripO
  }

  val debugStates = false

  def run(initial: BasicState = Pristine): Process[Task, Message] = {
    if (debugStates)
      current.discrete
        .map(z ⇒ log.debug(z.toString))
        .infraRunAsync("debug state printer")
    send(SetInitialState(initial))
    fsmProc.onComplete {
      running.setter(false).map(_ ⇒ MachineTerminated(this))
    }
  }

  def send(msg: Message) = {
    sendAll(msg.wrapNel)
  }

  def sendAll(msgs: NonEmptyList[Message]) = {
    internalMessageIn.enqueueAll(msgs.toList) !? "enqueue state messages"
  }

  // kill the state machine by
  // the 'term' signal is woven into the main process using the wye combinator
  // 'interrupt', which listens asynchronously and terminates instantly
  def kill() = {
    term.set(true) !? "set signal term"
    externalMessageIn.kill !? "kill external message queue"
    internalMessageIn.kill !? "kill internal message queue"
  }

  // gracefully shut down the state maching
  // the 'quit' signal uses the deterministic tee combinator 'until', which is
  // only emitted when the 'idle' signal is true
  // 'idle' is set when no actions are resent in unsafePerformIO
  // *> combines the two Task instances via Apply.apply2
  // roughly equivalent to a flatMap(_ ⇒ b)
  def join() = {
    log.trace(s"terminating $this")
    (quit.set(true) *> finished.run)
      .infraRunFor("wait for finished signal", 20 seconds)
  }

  def finished = running.continuous.exists(!_)

  private[this] def size = {
    externalMessageIn.size.discrete
      .yipWith(internalMessageIn.size.discrete)(_ + _)
      .take(1)
  }

  private[this] def waitingTasks = {
    size.runLast.attemptRun | None | 0
  }

  def description = s"$handle state"

  def handle: String

  override def toString = s"$description ($waitingTasks waiting)"

  override val loggerName = s"state.$handle".some

  lazy val internalMessage: Admission = {
    case SetInitialState(s) ⇒ setInitialState(s)
  }

  def setInitialState(s: BasicState): Transit = {
    case S(Pristine, d) ⇒ S(s, d)
  }

  lazy val unmatchedMessage: Admission = {
    case m if debugStates ⇒ {
      case s ⇒
        log.debug(s"unmatched message at $s: $m")
        s
    }
  }

  def unmatchedState(m: Message): Transit = {
    case s if debugStates ⇒
      log.debug(s"unmatched state $s: $m")
      s
  }
}

trait DroidMachineBase[A <: AndroidUiContext]
extends Machine[HNil]
{
  implicit def ctx: A
}

abstract class DroidMachine[A <: AndroidUiContext]
(implicit val ctx: A, mt: MessageTopic)
extends DroidMachineBase[A]

trait SimpleDroidMachine
extends DroidMachine[AndroidUiContext]

abstract class DroidMachineEC(implicit val ec: EC, ctx: AndroidUiContext,
  mt: MessageTopic)
extends SimpleDroidMachine

trait ActivityDroidMachine
extends DroidMachine[AndroidActivityUiContext]

abstract class DroidDBMachine
(implicit ec: EC, db: tryp.slick.DbInfo, ctx: AndroidActivityUiContext,
  mt: MessageTopic)
extends DroidMachineEC
