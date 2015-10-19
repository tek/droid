package tryp
package droid

import concurrent.duration._

import argonaut._, Argonaut._

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
  trait Loggable extends Message
  case class LogError(msg: String) extends Loggable
  case object Done extends Message
  case object NopMessage extends Message
  case class Toast(msg: String) extends Message
  case class ForkedResult(reason: String) extends Message

  case class LogFatal(description: String, error: Throwable)
  extends Loggable
  {
    lazy val message = Error.withTrace(s"exception while $description", error)
  }

  case class UnknownResult[A: Show](result: A)
  extends Loggable
  {
    def showResult = result.show
  }

  trait MessageInstances
  {
    implicit val messageShow = new Show[Message] {
      override def show(msg: Message) = {
        msg match {
          case res @ UnknownResult(result) ⇒
            Cord(s"${res.className}(${res.showResult})")
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
    def error(a: A) = LogError(a.toString)
  }
}

trait ToMessageInstances
extends ToMessageInstances0
{
  implicit def showableToMessage[A: Show] = new ToMessage[A] {
    def message(a: A) = UnknownResult(a)
    def error(a: A) = LogError(a.toString)
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
{
  // implicit conversions to Task[Result]
  // from Ui, DBIOAction, Message
  implicit def uiToTask(ui: Ui[Result]): Task[Result] =
    Task.suspend(ui.run.task)

  // TODO really block the machine while the Ui is running?
  implicit def anyUiToTask[A: ToMessage](ui: Ui[A])
  (implicit ec: EC): Task[Result] =
    Task.suspend {
      (ui.run: Future[A]).map(_.toMessage.toResult).task
    }

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
    action.map(_.bimap(_.map(e ⇒ e.toErrorMessage), _.toMessage))
      .task
  }

  implicit def liftMessage[A <: Message](m: A): Task[Result] =
    Task.now(m.successNel[Message])

  def debug[A]: Sink[Task, A] = sink.lift { (a: A) ⇒ Task(Log.i(a)) }

  def infraResult[A](desc: String)(res: \/[Throwable, A]) = {
    res match {
      case -\/(e) ⇒ Log.e(s"failed to $desc: $e")
      case _ ⇒
    }
  }

  implicit class ViewProcessOps[+O](proc: Process[Task, O])
  {
    def debug1[A] = proc |> await1[O] to debug[O]

    def !! = proc.runLog.run
  }

  implicit class ViewTaskOps[A](task: Task[A])
  {
    def infraRun(desc: String) = infraResult(desc)(task.attemptRun)
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
  (error: Throwable ⇒ Maybe[C])
  : stream.Writer1[C, A, B] = {
    receive1 { (a: A) ⇒
      val (effect, newState) = Try(f lift(a) flatMap(_.lift(z))) match {
        case Success(Some((newState, effect))) ⇒ effect.just → newState.just
        case Success(None) ⇒ Empty[C]() → Empty[B]()
        case Failure(t) ⇒ error(t) → Empty[B]()
      }
      val o = newState.map(Process.emitO) | Process.halt
      val w = effect.map(Process.emitW) | Process.halt
      o ++ w ++ scanSplitW(newState | z)(f)(error)
    }
  }

  def fatalTransition(t: Throwable): Maybe[List[AppEffect]] = {
    List(LogFatal("transitioning state", t): AppEffect).just
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
    def <<[A: ToAppEffect](e: Option[A]) = (f, Nil) << e
  }

  implicit class ViewTransitionResultOps(r: ViewTransitionResult)
  {
    def <<(e: AppEffect) = (r.head, r.last :+ e)
    def <<(e: Option[AppEffect]): ViewTransitionResult = e some(r.<<) none(r)
    def <<[A: ToAppEffect](e: Option[A]): ViewTransitionResult = {
      r << (e map(a ⇒ (a: AppEffect)))
    }
  }

  implicit def amendZthulhuWithEmptyEffects
  (f: Zthulhu): ViewTransitionResult =
    (f, Nil)
}

import Zthulhu._

abstract class StateImpl
(implicit ec: EC, db: tryp.slick.DbInfo, ctx: tryp.UiContext[_],
  broadcast: Broadcaster)
extends ToUiOps
with ViewStateImplicits
{
  val S = Zthulhu

  val transitions: ViewTransitions

  private[this] val messages = async.unboundedQueue[Message]

  private[this] val running = async.signalOf(false)

  private[this] val quit = async.signalOf(false)

  private[this] val term = async.signalOf(false)

  private[this] val idle: Process[Task, Boolean] =
    messages.size.continuous map(_ == 0)

  // TODO log results
  private[this] def unsafePerformIO(effects: List[AppEffect]) = {
    Task[Unit] {
      Log.d(s"performing io for ${effects.length} tasks")
      val next = effects
        .map(_.attemptRun)
        .flatMap {
          case \/-(m) ⇒
            Log.d(s"task succeeded with ${m.show}")
            m fold(_.toList, List(_))
          case -\/(t) ⇒
            List(LogFatal("performing io", t))
        }
      next match {
        case head :: tail ⇒ broadcast ! NonEmptyList(head, tail: _*)
        case Nil ⇒
      }
    }
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
      Log.i(s"FSM terminated: $a")
  }

  def send(msg: Message) = {
    sendAll(msg.wrapNel)
  }

  def sendAll(msgs: NonEmptyList[Message]) = {
    Log.d(s"sending ${msgs.show} to $description")
    messages.enqueueAll(msgs.toList).attemptRun.swap.foreach { e ⇒
      Log.e(s"failed to enqueue state messages: $e")
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
    Log.d(s"terminating $this")
    quit.set(true) *> finished.run attemptRunFor(20 seconds)
  }

  def finished = running.continuous.exists(!_)

  private[this] def size = messages.size.continuous

  private[this] def waitingTasks = size.take(1).!!.headOption.getOrElse(0)

  def description = this.className

  override def toString = s"$description ($waitingTasks waiting)"
}

trait Stateful
extends DbAccess
with ViewStateImplicits
{
  implicit val uiCtx: AndroidUiContext[Unit]

  implicit val broadcast: Broadcaster = new Broadcaster(sendAll)

  val logImpl = new StateImpl
  {
    override def description = "log state"

    def logError(msg: String): ViewTransition = {
      case s ⇒
        Log.e(msg)
        s
    }

    def logInfo(msg: String): ViewTransition = {
      case s ⇒
        Log.i(msg)
        s
    }

    val transitions: ViewTransitions = {
      case LogError(msg) ⇒ logError(msg.toString)
      case m @ LogFatal(_, _) ⇒ logError(m.message)
      case UnknownResult(msg) ⇒ logInfo(msg.toString)
      case m: Loggable ⇒ logInfo(m.toString)
    }
  }

  def impls: List[StateImpl] = logImpl :: Nil

  def allImpls[A](f: StateImpl ⇒ A) = impls map(f)

  def send(msg: Message) = allImpls(_.send(msg))

  def sendAll(msgs: NonEmptyList[Message]) = msgs foreach(send)

  val ! = send _

  def runState() = allImpls(_.runFsm())

  def killState() {
    allImpls(_.kill())
  }

  def joinState() {
    allImpls(_.join())
  }
}

abstract class StatefulFragment
extends TrypFragment
with Stateful
{
  override implicit val uiCtx: AndroidUiContext[Unit] = AndroidFragmentUiContext.default[Unit](this)

  // TODO impl log level
  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    runState()
    // logImpl ! LogLevel(LogLevel.DEBUG)
    send(Create(arguments, Option(saved)))
  }

  override def onResume() {
    super.onResume()
    send(Resume)
  }
}

trait StatefulActivity
extends TrypActivity
with Stateful
{
  val uiCtx = AndroidActivityUiContext.default[Unit]

  // TODO impl log level
  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    runState()
    // logImpl ! LogLevel(LogLevel.DEBUG)
    send(Create(Map(), Option(saved)))
  }

  override def onResume() {
    super.onResume()
    send(Resume)
  }
}

import UiActionTypes._

abstract class ShowStateImpl[A <: Model: DecodeJson]
(implicit ec: EC, db: tryp.slick.DbInfo, ctx: tryp.UiContext[_],
  broadcast: Broadcaster)
extends StateImpl
{
  case class Model(model: A)
  extends Data

  case class SetModel(model: A)
  extends Message

  def name: String

  override def description = s"state for show $name"

  def setupData(args: Map[String, String]) = {
    def errmsg(item: String) = {
      s"No valid $item passed to show impl for '$name'"
    }
    args.get(Keys.model)
      .flatMap(_.decodeOption[A])
      .toDbioSuccess
      .nelM(errmsg("model"))
      .orElse {
        args.get(Keys.dataId)
          .flatMap(id ⇒ Try(ObjectId(id)).toOption)
          .toDbioSuccess
          .nelM(errmsg("dataId"))
          .nelFlatMap { a ⇒
            fetchData(a) nelM(s"fetchData failed for $a")
          }
      }
      .vmap(SetModel.apply)
  }

  def fetchData(id: ObjectId): AnyAction[Option[A]]

  def fetchDetails(m: A): AppEffect = Nop

  def updateData(m: A): AppEffect

  def create(args: Map[String, String], state: Option[Bundle])
  : ViewTransition = {
    case S(Pristine, data) ⇒
      S(Initializing, data) << setupData(args)
  }

  val resume: ViewTransition = {
    case s @ S(_, _) ⇒
      s
  }

  def model(m: A): ViewTransition = {
    case S(Initializing, data) ⇒
      S(Initialized, Model(m)) << Update
  }

  def update: ViewTransition = {
    case s @ S(Initialized, Model(m)) ⇒
      s << updateData(m) << fetchDetails(m)
  }

  val catchall: ViewTransition = {
    case s ⇒
      p("catchall")
      s
  }

  val transitions: ViewTransitions = {
    case Create(args, state) ⇒ create(args, state)
    case Resume ⇒ resume
    case Update ⇒ update
    case SetModel(m) ⇒ model(m)
    case _ ⇒ catchall
  }
}
