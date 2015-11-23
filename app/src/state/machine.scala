package tryp
package droid

import java.util.concurrent.Executors

import concurrent.duration._

import scalaz.{Writer ⇒ _, _}, Scalaz._, concurrent._, stream._, Process._
import async.mutable.{Signal, Queue, Topic}

import shapeless.syntax.std.tuple._

import org.log4s.Logger

object State
{
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

  case class EffectSuccessful(description: String, result: Any)
  extends Loggable
  {
    lazy val message = s"successful effect: $description ($result)"
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
  }

  object Message
  extends MessageInstances

  trait Data
  case object NoData extends Data

  type Result = ValidationNel[Message, Message]
  type AppEffect = Process[Task, Result]

  type ViewTransitionResult = (Zthulhu, AppEffect)

  type ViewTransition = PartialFunction[Zthulhu, ViewTransitionResult]

  type ViewTransitions = PartialFunction[Message, ViewTransition]

  type TransitionsSelection = Message ⇒ ViewTransitions

  val Nop = emit(NopMessage.successNel)

  def signalSetter[A] = process1.lift[A, Signal.Set[A]](Signal.Set(_))
}
import State._
import Message._

trait ToMessage[A]
{
  def message(a: A): Message
  def error(a: A): Message
}

trait ToMessageInstances0
{
  implicit def anyToMessage[A] = new ToMessage[A] {
    def message(a: A) = {
      UnknownResult(a.toString)
    }
    def error(a: A) = LogError("running unknown task", a.toString)
  }
}

trait ToMessageInstances
extends ToMessageInstances0
{
  implicit def showableToMessage[A: Show] = new ToMessage[A] {
    def message(a: A) = UnknownResult(a)
    def error(a: A) = LogError("running unknown task", a.toString)
  }
}

object ToMessage
extends ToMessageInstances
{
  implicit def messageToMessage[A <: Message] = new ToMessage[A] {
    def message(a: A) = a
    def error(a: A) = a
  }

  implicit def futureToMessage[A] = new ToMessage[ScalaFuture[A]] {
    private[this] def fail = sys.error("Tried to convert future to message")
    def message(f: Future[A]) = fail
    def error(f: Future[A]) = fail
  }
}

object ToMessageSyntax
{
  implicit class ToMessageOps[A](a: A)(implicit tm: ToMessage[A]) {
    def toErrorMessage = tm.error(a)
    def toMessage = tm.message(a)
  }
}
import ToMessageSyntax._

trait Operation[A]
{
  def result(a: A): Result
}

final class OperationSyntax[A](a: A)(implicit op: Operation[A])
{
  def toResult = op.result(a)
}

trait ToOperationSyntax
{
  implicit def ToOperationSyntax[A: Operation](a: A) = new OperationSyntax(a)
}

trait OperationInstances0
extends ToOperationSyntax
{
  implicit lazy val messageOperation = new Operation[Message] {
    def result(m: Message) = m.successNel[Message]
  }

  implicit def anyOperation[A] = new Operation[A] {
    def result(a: A) = a.toMessage.toResult
  }
}

trait OperationInstances
extends OperationInstances0
{
  implicit def validationNelOperation[E: ToMessage, A: ToMessage] =
    new Operation[ValidationNel[E, A]] {
      def result(v: ValidationNel[E, A]): Result = {
        v.bimap(_.map(e ⇒ e.toErrorMessage), _.toMessage)
      }
    }
}

object Operation
extends OperationInstances

trait ToProcess[F[_]]
{
  def proc[A](fa: F[A]): Process[Task, A]
}

object ToProcess
{
  implicit def anyActionToProcess(implicit ec: EC, dbi: DbInfo) =
    new ToProcess[AnyAction] {
      def proc[A](aa: AnyAction[A]) = aa.proc
    }
}

trait ToProcessSyntax
{
  implicit class ToToProcess[A, F[_]](fa: F[A])(implicit tp: ToProcess[F])
  {
    def proc = tp.proc(fa)
  }
}

// TODO add UiAction implicits
trait StateImplicits
extends Logging
with ToOperationSyntax
with ToProcessSyntax
{
  implicit def liftProcess[A: Operation](proc: Process[Task, A]) = {
    proc map(a ⇒ a.toResult)
  }

  implicit def liftTask[A: Operation](t: Task[A]): AppEffect =
    Process.eval(t map(_.toResult))

  implicit def functorToAppEffect[A: Operation, F[_]: Functor: ToProcess]
  (fa: F[A]) = {
    new ToToProcess(fa map(_.toResult)) proc
  }

  // implicit conversions to Task[Result]
  // from Ui, DBIOAction, Message
  implicit def uiToTask(ui: Ui[Result]): AppEffect =
    Process(UiTask(ui))

  // TODO really block the machine while the Ui is running?
  implicit def anyUiToTask[A: ToMessage](ui: Ui[A])
  (implicit ec: EC): AppEffect = {
    uiToTask(ui map(_.toMessage))
  }

  implicit def snailToTask[A](ui: Ui[ScalaFuture[A]])
  (implicit ec: EC): AppEffect =
    Task {
      ui.run
      ForkedResult("Snail").toResult
    }

  implicit def tryToTask[A: ToMessage](t: Try[A]): AppEffect = {
    t match {
      case Success(a) ⇒ a.toMessage
      case Failure(e) ⇒ LogFatal("evaluating try", e)
    }
  }

  implicit def liftOptionTask(t: Task[Option[Result]]): AppEffect = {
    t map {
      case Some(e) ⇒ e
      case None ⇒ LogVerbose("task returned None")
    }
  }

  implicit def liftResult(r: Result): AppEffect = Process(r)

  implicit def liftMessage[A <: Message](m: A): Result =
    m.successNel[Message]

  implicit def messageToAppEffect[A <: Message](m: A): AppEffect = {
    (m: Result): AppEffect
  }

  implicit final class TaskOps[A](task: Task[A])
  {
    def effect: AppEffect = {
      task map(r ⇒ liftMessage(
        EffectSuccessful("implicitly converted task", r.toString)
      ))
    }
  }

  type ToAppEffect[A] = A ⇒ AppEffect

  implicit def foldableToAppEffect[A: ToAppEffect, F[_]: Foldable]
  (fa: F[A]) = (fa foldLeft(halt: AppEffect)) {
    case (z, a) ⇒ z ++ (a: AppEffect)
  }

  def stateEffectProc[F[_], O](description: String)(effect: Process[F, O]) = {
    effect map(EffectSuccessful(description, _).toResult)
  }

  def stateEffectTask[F[_], O](description: String)(effect: F[O]) = {
    stateEffectProc(description)(Process.eval(effect))
  }

  def stateEffect(description: String)(effect: ⇒ Unit) = {
    stateEffectTask(description)(Task(effect))
  }

  implicit final class ProcessOps[F[_], A](proc: Process[F, A])
  {
    def logged(desc: String)(implicit logger: Logger) = {
      proc |> process1.lift { m ⇒
        logger.trace(s"$desc delivering $m")
        m
      }
    }
  }

  implicit final class QueueOps[A](queue: Queue[A])
  {
    def publish(desc: String)(topic: Topic[A]) = {
      queue.dequeue
        .logged(desc)
        .to(topic.publish)
        .run
        .infraRunAsync(desc)
    }
  }

  implicit def messageProcMonoid =
    Monoid.instance[Process[Task, Message]]((a, b) ⇒ a.merge(b), halt)
}

object StateImplicits
extends StateImplicits

case class Zthulhu(state: BasicState = Pristine, data: Data = NoData)

object Zthulhu
extends StateImplicits
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
  : stream.Writer1[Process[F, E], M, Z] = {
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
  (f: Zthulhu): ViewTransitionResult =
    (f, halt)

  implicit class ViewTransitionResultOps(r: ViewTransitionResult)
  {
    def <<[A: ToAppEffect](e: A): ViewTransitionResult = {
      (r.head, r.last ++ e: AppEffect)
    }
  }

  implicit class ZthulhuOps(z: Zthulhu)
  {
    def <<[A: ToAppEffect](e: A) = (z: ViewTransitionResult) << e
  }
}

import Zthulhu._

abstract class StateImpl(implicit messageTopic: MessageTopic)
extends ToUiOps
with StateImplicits
with Logging
{
  implicit private[this] lazy val executor = Executors.newFixedThreadPool(5)

  implicit private[this] lazy val strat: Strategy = Strategy.Executor

  val S = Zthulhu

  def transitions: ViewTransitions

  private[this] val internalMessageIn = async.unboundedQueue[Message]

  private[this] val externalMessageIn = async.unboundedQueue[Message]

  private[this] def messageIn = {
    internalMessageIn.dequeue.logged(s"$description internal")
      .merge(externalMessageIn.dequeue.logged(s"$description external"))
  }

  val messageOut = async.unboundedQueue[Message]

  private[this] val running = async.signalOf(false)

  private[this] val quit = async.signalOf(false)

  private[this] val term = async.signalOf(false)

  val current = async.signalUnset[Zthulhu]

  private[this] val idle: Process[Task, Boolean] =
    size map(_ == 0)

  private[this] lazy val unsafePerformIO: Process1[Result, Message] = {
    process1
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

  protected def transitionsSelection: Message ⇒ ViewTransitions = {
    case m: InternalMessage ⇒ internalMessage
    case m: Message ⇒ transitions
  }

  protected def uncurriedTransitions(z: Zthulhu, m: Message) = {
    transitionsSelection(m)
      .orElse(unmatchedMessage)
      .lift(m)
      .getOrElse(unmatchedState(m))
      .lift(z)
      .toMaybe
  }

  private[this] lazy val fsmProc: Writer[Task, Message, Zthulhu] = {
    term.discrete
      .merge(idle.when(quit.discrete))
      .wye(messageIn)(wye.interrupt)
      .viewFsm(uncurriedTransitions, unsafePerformIO)
      .observeO(current.sink.pipeIn(signalSetter[Zthulhu]))
      .observeW(messageOut.enqueue)
  }

  private[this] lazy val fsmTask = fsmProc.runLog

  val debugStates = false

  def runFsm(initial: BasicState = Pristine) = {
    running.set(true) !? "set sig running"
    (messageTopic.subscribe to externalMessageIn.enqueue)
      .run
      .infraRunAsync("message input queue")
    fsmTask.runAsync {
      case a ⇒
        running.set(false) !? "unset sig running"
        log.info(s"FSM terminated: $a")
    }
    if (debugStates)
      current.discrete
        .map(z ⇒ log.debug(z.toString))
        .run
        .infraRunAsync("debug state printer")
    send(SetInitialState(initial))
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
    quit.set(true) *> finished.run attemptRunFor(20 seconds)
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

  lazy val internalMessage: ViewTransitions = {
    case SetInitialState(s) ⇒ setInitialState(s)
  }

  def setInitialState(s: BasicState): ViewTransition = {
    case S(Pristine, d) ⇒ S(s, d)
  }

  lazy val unmatchedMessage: ViewTransitions = {
    case m if debugStates ⇒ {
      case s ⇒
        log.debug(s"unmatched message at $s: $m")
        s
    }
  }

  def unmatchedState(m: Message): ViewTransition = {
    case s if debugStates ⇒
      log.debug(s"unmatched state $s: $m")
      s
  }
}

trait DroidStateBase[A <: AndroidUiContext]
extends StateImpl
{
  implicit def ctx: A
}

abstract class DroidState[A <: AndroidUiContext]
(implicit val ctx: A, mt: MessageTopic)
extends DroidStateBase[A]

trait SimpleDroidState
extends DroidState[AndroidUiContext]

abstract class DroidStateEC(implicit val ec: EC, ctx: AndroidUiContext,
  mt: MessageTopic)
extends SimpleDroidState

trait ActivityDroidState
extends DroidState[AndroidActivityUiContext]

abstract class DroidDBState
(implicit ec: EC, db: tryp.slick.DbInfo, ctx: AndroidActivityUiContext,
  mt: MessageTopic)
extends DroidStateEC
