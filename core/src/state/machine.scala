package tryp
package droid

import concurrent.duration._

import scalaz._, Scalaz._
import concurrent.{Task, Actor}
import stream._
import Process._

import shapeless.syntax.std.tuple._

object ViewState
{
  trait BasicState
  case object Pristine extends BasicState
  case object Initialized extends BasicState
  case object Initializing extends BasicState

  trait Message
  {
    def toResult = this.successNel[Message]
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

  case class EffectSuccessful(description: String)
  extends Loggable
  {
    lazy val message = s"successful effect: $description"
  }

  def stateEffect(description: String)(effect: ⇒ Unit) = {
    Task(effect) map(_ ⇒ EffectSuccessful(description).success)
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

  // TODO type class Operation that converts to Task[Result]
  type Result = ValidationNel[Message, Message]
  type AppEffect = Task[Result]

  type ViewTransitionResult = (Zthulhu, List[AppEffect])

  type ViewTransition = PartialFunction[Zthulhu, ViewTransitionResult]

  type ViewTransitions = PartialFunction[Message, ViewTransition]

  type ViewEffects[A[_]] = List[AppEffect] ⇒ A[Unit]

  val Nop = Task.now(NopMessage.successNel)

  type Broadcaster = Actor[NonEmptyList[Message]]
}
import ViewState._
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

// TODO add UiAction implicits
trait ViewStateImplicits
extends Logging
{
  implicit def validationNelToResult[E: ToMessage, A: ToMessage]
  (v: ValidationNel[A, E]): Result = {
    v.bimap(_.map(e ⇒ e.toErrorMessage), _.toMessage)
  }

  // implicit conversions to Task[Result]
  // from Ui, DBIOAction, Message
  implicit def uiToTask(ui: Ui[Result]): Task[Result] =
    Task(UiTask(ui))

  // TODO really block the machine while the Ui is running?
  implicit def anyUiToTask[A: ToMessage](ui: Ui[A])
  (implicit ec: EC): Task[Result] =
    uiToTask(ui map(_.toMessage))

  implicit def snailToTask[A](ui: Ui[ScalaFuture[A]])
  (implicit ec: EC): Task[Result] =
    Task {
      ui.run
      ForkedResult("Snail").toResult
    }

  implicit def actionToTask(action: AnyAction[Result])
  (implicit dbi: DbInfo, ec: EC): Task[Result] = action.task

  implicit def anyActionToTask[E: ToMessage, A: ToMessage]
  (action: AnyAction[ValidationNel[E, A]])
  (implicit dbi: DbInfo, ec: EC): Task[Result] = {
    action.map(a ⇒ a: Result)
      .task
  }

  implicit def tryToTask[A: ToMessage](t: Try[A]): AppEffect = {
    t match {
      case Success(a) ⇒ a.toMessage
      case Failure(e) ⇒ LogFatal("evaluating try", e)
    }
  }

  implicit def liftResult(r: Result): AppEffect = Task.now(r)

  implicit def liftMessage[A <: Message](m: A): Result =
    m.successNel[Message]

  implicit def messageToAppEffect[A <: Message](m: A): AppEffect = {
    (m: Result): AppEffect
  }

  implicit class ViewProcessOps[+O](proc: Process[Task, O])
  {
    // TODO don't log in production?
    def !! = proc.runLog.run
  }
}

object ViewStateImplicits
extends ViewStateImplicits

import ViewStateImplicits._

case class Zthulhu(state: BasicState = Pristine, data: Data = NoData)

object Zthulhu
{
  /* @param z current state machine
   * @param f A ⇒ B ⇒ (B, C), transforms a message into a state transition
   * function.
   * the state of type B is transitioned into a new state of type B and a
   * set of effects that has type C.
   * first, receive one item of type A representing a Message
   * if f is defined at that item and the transition function is defined for
   * the current state z, the result of that transition is calculated
   * if successful, the state z is emitted as output, the effects returned from
   * the transition function are emitted as log and the current state z is
   * replaced by the return value of the transition function
   * in any case, the new or old state is used to recurse
   *
   */
  def scanSplitW[A, B, C]
  (z: B)
  (f: PartialFunction[A, PartialFunction[B, (B, C)]])
  (error: (B, A, Throwable) ⇒ Maybe[C])
  : stream.Writer1[C, A, B] = {
    receive1 { (a: A) ⇒
      val (effect, newState) = Try(f lift(a) flatMap(_.lift(z))) match {
        case Success(Some((newState, effect))) ⇒ effect.just → newState.just
        case Success(None) ⇒ Empty[C]() → Empty[B]()
        case Failure(t) ⇒ error(z, a, t) → Empty[B]()
      }
      val o = newState.map(Process.emitO) | Process.halt
      val w = effect.map(Process.emitW) | Process.halt
      o ++ w ++ scanSplitW(newState | z)(f)(error)
    }
  }

  def fatalTransition(state: Zthulhu, cause: Message, t: Throwable)
  : Maybe[List[AppEffect]] = {
    List(LogFatal(s"transitioning $state for $cause", t): AppEffect).just
  }

  implicit class ProcessToZthulhu[A[_]](source: Process[A, Message])
  {
    def viewFsm(transition: ViewTransitions, effect: ViewEffects[A]) = {
      (source |> scanSplitW(Zthulhu())(transition)(fatalTransition))
        .observeW(sink.lift(effect))
        .stripW
    }
  }

  type ToAppEffect[A] = A ⇒ AppEffect

  implicit class ZthulhuOps(f: Zthulhu)
  {
    def <<(e: AppEffect) = (f, Nil) << e
    def <<(e: Option[AppEffect]) = (f, Nil) << e
    def <<[A: ToAppEffect](e: A) = (f, Nil) << e
    def <<[A: ToAppEffect](e: Option[A]) = (f, Nil) << e
  }

  implicit class ViewTransitionResultOps(r: ViewTransitionResult)
  {
    def <<(e: AppEffect) = (r.head, r.last :+ e)
    def <<(e: Option[AppEffect]): ViewTransitionResult = e some(r.<<) none(r)
    def <<[A: ToAppEffect](e: A): ViewTransitionResult = {
      r << (e: AppEffect)
    }
    def <<[A: ToAppEffect](e: Option[A]): ViewTransitionResult = {
      r << (e map(a ⇒ (a: AppEffect)))
    }
  }

  implicit def amendZthulhuWithEmptyEffects
  (f: Zthulhu): ViewTransitionResult =
    (f, Nil)
}

import Zthulhu._

trait StateImpl
extends ToUiOps
with ViewStateImplicits
with Logging
{
  val S = Zthulhu

  val transitions: ViewTransitions

  private[this] val messages = async.unboundedQueue[Message]

  private[this] val running = async.signalOf(false)

  private[this] val quit = async.signalOf(false)

  private[this] val term = async.signalOf(false)

  private[this] val idle: Process[Task, Boolean] =
    messages.size.continuous map(_ == 0)

  private[this] def unsafePerformIO(effects: List[AppEffect]) = {
    Task[Unit] {
      log.trace(s"performing io for ${effects.length} tasks")
      val next = effects
        .map(_.attemptRun)
        .flatMap {
          case \/-(m) ⇒
            log.debug(s"task succeeded with ${m.show}")
            m.fold(_.toList, List(_))
          case -\/(t) ⇒
            List(LogFatal("performing io", t))
        }
      next match {
        case head :: tail ⇒
          val msgs = NonEmptyList(head, tail: _*)
          log.trace(s"broadcasting ${msgs.show}")
          dispatchResults(msgs)
        case Nil ⇒
      }
    }
  }

  def dispatchResults(msgs: NonEmptyList[Message]) = {
    sendAll(msgs)
  }

  private[this] lazy val fsmProc: Process[Task, Zthulhu] = {
    running.set(true).infraRun("set sig running")
    term.discrete
      .merge(idle.when(quit.discrete))
      .wye(messages.dequeue)(wye.interrupt)
      .viewFsm(transitions, unsafePerformIO)
  }

  private[this] lazy val fsmTask = fsmProc.runLog

  def runFsm() = fsmTask.runAsync {
    case a ⇒
      running.set(false).infraRun("unset sig running")
      log.info(s"FSM terminated: $a")
  }

  def send(msg: Message) = {
    sendAll(msg.wrapNel)
  }

  def sendAll(msgs: NonEmptyList[Message]) = {
    messages.enqueueAll(msgs.toList).attemptRun.swap.foreach { e ⇒
      log.error(s"failed to enqueue state messages: $e")
    }
  }

  // kill the state machine
  // the 'term' signal is woven into the main process using the wye combinator
  // 'interrupt', which listens asynchronously and terminates instantly
  def kill() = {
    messages.kill.attemptRunFor(5 seconds)
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

  private[this] def size = messages.size.continuous

  private[this] def waitingTasks = size.take(1).!!.headOption.getOrElse(0)

  def description = s"$handle state"

  def handle: String

  override def toString = s"$description ($waitingTasks waiting)"

  override val loggerName = s"state.$handle".some

abstract class DroidState
(implicit val ec: EC, db: tryp.slick.DbInfo, val ctx: AndroidUiContext,
  broadcast: Broadcaster)
extends StateImpl
{
  override def dispatchResults(msgs: NonEmptyList[Message]) = broadcast ! msgs
}
