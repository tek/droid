package tryp
package droid

import argonaut._, Argonaut._

import scalaz._, Scalaz._
import stream._
import Process._
import scalaz.concurrent.Task

object ViewEvents
{
  sealed trait BasicState
  case object Pristine extends BasicState
  case object Initialized extends BasicState
  case object Initializing extends BasicState

  trait Message
  case class Create(args: Map[String, String], state: Option[Bundle])
  extends Message
  case object Resume extends Message
  case object Update extends Message

  trait Data
  case object NoData extends Data

  case class ViewFsm(state: BasicState = Pristine, data: Data = NoData)

  type AppEffect = Either3[AnyUi, DbAction, Message]

  type ViewTransitionResult = (ViewFsm, List[AppEffect])

  type ViewTransition = PartialFunction[ViewFsm, ViewTransitionResult]

  type ViewTransitions = Message ⇒ ViewTransition

  type ViewEffects[A[_]] = List[AppEffect] ⇒ A[Unit]

  def scanSplitW[A, B, C](z: B)(f: (A, B) ⇒ (B, C)): stream.Writer1[C, A, B] =
    receive1 { (a: A) ⇒
      val (next, effect) = f(a, z)
      Process.emitO(z) ++ Process.emitW(effect) ++ scanSplitW(next)(f)
    }

  implicit class ProcessToViewFsm[A[_]](source: Process[A, Message])
  {
    def viewFsm(transition: ViewTransitions, effect: ViewEffects[A]) = {
      (source |> scanSplitW(ViewFsm())(Function.uncurried(transition)))
        .observeW(sink.lift(effect))
        .stripW
    }
  }

  implicit def amendViewFsmWithEmptyEffects
  (f: ViewFsm): ViewTransitionResult =
    (f, Nil)

  implicit class ViewFsmOps(f: ViewFsm)
  {
    def <<[A >: AnyUi with DbAction with Message](effects: A*) = {
      (f, effects.toList collect {
        case u: AnyUi ⇒ Left3(u)
        case a: DbAction ⇒ Middle3(a)
        case m: Message ⇒ Right3(m)
      })
    }
  }
}
import ViewEvents._

abstract class StatefulFragment
extends MainFragment
with DbAccess
{
  type I <: Impl

  implicit def ctx = AndroidActivityUiContext.default

  val impl: I

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    impl.runFsm()
    impl.send(Create(getArguments.toMap, Option(saved)))
  }

  override def onResume() {
    super.onResume()
    impl.send(Resume)
  }
}

abstract class Impl(implicit ec: EC, db: tryp.slick.DbInfo, ctx: tryp.UiContext[_])
{
  val S = ViewFsm

  val transitions: ViewTransitions

  private[this] val messages = async.unboundedQueue[Message]

  private[this] val quit = async.signalUnset[Boolean]

  // TODO log results
  private[this] def unsafePerformIo(effects: List[AppEffect]) = {
    Task[Unit] {
      effects collect {
        case Left3(ui) ⇒ new UiOps(ui).attemptUi
        case Middle3(action) ⇒ action.asTry.!!
        case Right3(message) ⇒ send(message)
      }
    }
  }

  private[this] lazy val fsmProc: Process[Task, ViewFsm] = {
    messages.dequeue.viewFsm(transitions, unsafePerformIo)
  }

  private[this] lazy val fsmTask = {
    fsmProc.run.runAsync {
      case _ ⇒ ()
    }
  }

  def runFsm() = fsmTask

  def send(msg: Message) = {
    hl
    p(s"sending $msg")
    messages.enqueueOne(msg).run
  }
}

import UiActionTypes._

abstract class ShowImpl[A <: Model: DecodeJson]
(implicit ec: EC, db: tryp.slick.DbInfo, ctx: tryp.UiContext[_])
extends Impl
{
  case class Model(model: A)
  extends BasicState

  def name: String

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
      .vmap(updateData)
  }

  private def initData(id: ObjectId) = {
    // Future { model = fetchData(id) } map(Unit ⇒ update())
  }

  def fetchData(id: ObjectId): AnyAction[Option[A]]

  def update() = {
    // model foreach { updateData(_).run }
    fetchDetails()
  }

  def fetchDetails() {}

  def updateData(m: A): Ui[Any]

  def create(args: Map[String, String], state: Option[Bundle])
  : ViewTransition = {
    case S(Pristine, data) ⇒
      p("initializing")
      S(Initializing, data) << setupData(args)
    case S(Initializing, data) ⇒
      S(Initialized, data) << Update
    case s @ S(Initialized, data) ⇒
      s
  }

  val resume: ViewTransition = {
    case s @ S(_, _) ⇒
      p("resuming")
      s
  }

  val catchall: ViewTransition = {
    case s ⇒
      p("catchall")
      s
  }

  val transitions: ViewTransitions = {
    case Create(args, state) ⇒ create(args, state)
    case Resume ⇒ resume
    case _ ⇒ catchall
  }
}
