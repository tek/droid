package tryp
package droid

import concurrent.duration._

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import stream._
import Process._
import scalaz.concurrent.Task

import shapeless.syntax.std.tuple._

object ViewEvents
{
  trait BasicState
  case object Pristine extends BasicState
  case object Initialized extends BasicState
  case object Initializing extends BasicState

  trait Message
  case class Create(args: Map[String, String], state: Option[Bundle])
  extends Message
  case object Resume extends Message
  case object Update extends Message
  case class LogError(msg: String) extends Message
  case class LogFatal(error: Throwable) extends Message
  case object Done extends Message
  case object NopMessage extends Message
  case class UnknownResult[A](result: A) extends Message
  case class Toast(msg: Option[String]) extends Message

  trait Data
  case object NoData extends Data

  case class Zthulhu(state: BasicState = Pristine, data: Data = NoData)

  // TODO type class Operation that converts to Task[Result]
  type Result = ValidationNel[Message, Message]
  type AppEffect = Task[Result]

  type ViewTransitionResult = (Zthulhu, List[AppEffect])

  type ViewTransition = PartialFunction[Zthulhu, ViewTransitionResult]

  type ViewTransitions = PartialFunction[Message, ViewTransition]

  type ViewEffects[A[_]] = List[AppEffect] ⇒ A[Unit]

  val Nop = Task.now(NopMessage.successNel)

  def scanSplitW[A, B, C]
  (z: B)
  (f: PartialFunction[A, PartialFunction[B, (B, C)]])
  : stream.Writer1[C, A, B] =
    receive1 { (a: A) ⇒
      f.lift(a)
        .flatMap(_.lift(z))
        .some { case (next, effect) ⇒
          Process.emitO(z) ++ Process.emitW(effect) ++ scanSplitW(next)(f)
        }
        .none(scanSplitW(z)(f))
    }

  implicit class ProcessToZthulhu[A[_]](source: Process[A, Message])
  {
    def viewFsm(transition: ViewTransitions, effect: ViewEffects[A]) = {
      (source |> scanSplitW(Zthulhu())(transition))
        .observeW(sink.lift(effect))
        .stripW
    }
  }
}
import ViewEvents._

trait ToMessage[A]
{
  def message(a: A): Message
  def error(a: A): Message
}

trait ToMessageLPI
{
  implicit def anyToMessage[A] = new ToMessage[A] {
    def message(a: A) = UnknownResult(a)
    def error(a: A) = LogError(a.toString)
  }
}

object ToMessage
extends ToMessageLPI
{
  implicit def messageToMessage = new ToMessage[Message] {
    def message(a: Message) = a
    def error(a: Message) = a
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
trait ViewEventsImplicits
{
  type ToAppEffect[A] = A ⇒ AppEffect

  implicit class ZthulhuOps(f: Zthulhu)
  {
    def <<(e: AppEffect) = (f, Nil) << e
    def <<[A: ToAppEffect](e: Option[A]) = (f, Nil) << e
  }

  implicit class ViewTransitionResultOps(r: ViewTransitionResult)
  {
    def <<(e: AppEffect) = (r.head, r.last :+ e)
    def <<[A: ToAppEffect](e: Option[A]) = {
      e some(ef ⇒ (r.head, r.last :+ (ef: AppEffect))) none(r)
    }
  }

  implicit def amendZthulhuWithEmptyEffects
  (f: Zthulhu): ViewTransitionResult =
    (f, Nil)

  implicit def uiToTask(ui: Ui[Result]): Task[Result] =
    Task.suspend(ui.run.task)

  implicit def anyUiToTask(ui: Ui[_])(implicit ec: EC): Task[Result] =
    Task.suspend {
      ui.run.map(_.toMessage.successNel[Message]).task
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

object ViewEventsImplicits
extends ViewEventsImplicits

abstract class StateImpl
(implicit ec: EC, db: tryp.slick.DbInfo, ctx: tryp.UiContext[_])
extends ToUiOps
with ViewEventsImplicits
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
      p(s"performing io for ${effects.length} tasks")
      val next = effects
        .map(_.attemptRun)
        .flatMap {
          case \/-(m) ⇒
            p(s"task succeeded with $m")
            m fold(_.toList, List(_))
          case -\/(t) ⇒
            p(s"task failed with $t")
            List(LogFatal(t))
        }
      next match {
        case head :: tail ⇒ sendAll(NonEmptyList(head, tail: _*))
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
    Log.d(s"sending $msgs to $description")
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

abstract class StatefulFragment
extends TrypFragment
with DbAccess
with ViewEventsImplicits
{
  implicit def ctx = AndroidActivityUiContext.default

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
      case LogFatal(msg) ⇒ logError(msg.toString)
      case UnknownResult(msg) ⇒ logInfo(msg.toString)
    }
  }

  def impls: List[StateImpl] = logImpl :: Nil

  def send(msg: Message) = impls foreach(_.send(msg))

  val ! = send _

  // TODO impl log level
  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    impls foreach(_.runFsm())
    // logImpl ! LogLevel(LogLevel.DEBUG)
    send(Create(arguments, Option(saved)))
  }

  override def onResume() {
    super.onResume()
    send(Resume)
  }

  def killState() {
    impls foreach(_.kill())
  }

  def joinState() {
    impls foreach(_.join())
  }
}

import UiActionTypes._

abstract class ShowStateImpl[A <: Model: DecodeJson]
(implicit ec: EC, db: tryp.slick.DbInfo, ctx: tryp.UiContext[_])
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
