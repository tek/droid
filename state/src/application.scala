package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity

import shapeless._, ops.hlist._

import fs2.async

import tryp.state.annotation._
import core.{AIORunner, ToAIORunner}
import MainViewMessages.Back

object DefaultScheduler
{
  implicit lazy val scheduler: Scheduler = Scheduler.fromFixedDaemonPool(4)
}

object StatePool
extends BoundedCachedPool
with SchedulerStrategyPool
with Logging
{
  def name = "state"

  implicit def scheduler = DefaultScheduler.scheduler
}
import StatePool._

case class AndroidState(act: Activity)
extends CState

@cell
object AndroidCell
extends AnnotatedTAIO
{
  def trans: Transitions = {
    case SetActivity(act) =>
      AndroidState(act) :: HNil
    case m: ContextAIO => {
      case AndroidState(act) => (ec: ExecutionContext) => IOStateIO(m.task(act), m.desc) :: HNil
    }
    case m: ActivityAIO => {
      case AndroidState(act) => (ec: ExecutionContext) => IOStateIO(m.task(act), m.desc) :: HNil
    }
    case m: AppCompatActivityAIO => {
      case AndroidState(act: AppCompatActivity) => (ec: ExecutionContext) => IOStateIO(m.task(act), m.desc) :: HNil
    }
    case m: StateActivityAIO => {
      case AndroidState(act: StateActivity) => (ec: ExecutionContext) => IOStateIO(m.task(act), m.desc) :: HNil
    }
    case SetContentTree(t) => actU(_.setContentView(t.container)) :: HNil
  }
}

trait AppState
extends Logging
{
  def loopCtor: IO[(LoopData.MQueue, Signal[Boolean], LoopData.OStream)]

  def mainView: MVContainer

  def ctor = {
    for {
      ito <- loopCtor
      (in, term, out) = ito
      state <- async.signalOf[IO, List[CState]](Nil)
      sink: fs2.Sink[IO, LoopData.Data] = _.collect{case Left(a) => a}.evalMap(state.set).drain
      // loop <- IO.start(out.observe(sink).runLog)
      loop <- IO(out.observe(sink).runLog)
    } yield (in, term, loop, state)
  }

  lazy val (in, term, loop, currentStates) = ctor.unsafeRunSync()

  def currentState = currentStates.get

  def unsafeCurrentState = currentState.unsafeRunSync()

  def run() = loop.unsafeRunAsync {
    case Left(a: Throwable) =>
      val trace = a.getStackTrace.toList.mkString("\n")
      log.info(s"state loop failed with $a:\n$trace")
    case a => log.info(s"state loop finished with $a")
  }

  def send(msg: Message): IO[Unit] = in.enqueue1(msg)

  def unsafeSend(msg: Message): Unit = send(msg).unsafeRunSync()

  // FIXME doesn't work, but doing the same from within the activity does
  def setActivity = SetActivity.apply _ andThen unsafeSend _
}

trait ExtMVAppState
extends AppState
{
  val mainView = ExtMVFrame.aux

  type AndroidCells = AndroidCell.Aux :: ExtMVFrame.Aux :: HNil
  def androidCells: AndroidCells = AndroidCell.aux :: mainView :: HNil

//   implicit def androidCellsTransition: transition.Case.Aux[AndroidCells, Message, SLR] =
//     transition.dyn
}

trait StateApplication
extends android.app.Application
{
  def state: AppState

  override def onCreate(): Unit = {
    state.run()
    super.onCreate()
  }

  def send = state.send _

  def unsafeSend = state.unsafeSend _

  def setActivity = state.setActivity _
}

abstract class StateActivity
extends AppCompatActivity
{
  lazy val stateApp = getApplication match {
    case a: StateApplication => a
    case _ => sys.error(s"application is not a StateApplication: $getApplication")
  }

  def appState = stateApp.state

  def initialMessages: List[Message]

  def send = appState.send _

  def unsafeSend = appState.unsafeSend _

  override def onCreate(state: Bundle) = {
    Thread.sleep(500)
    unsafeSend(SetActivity(this))
    super.onCreate(state)
    unsafeSend(CreateContentView)
    initialMessages foreach stateApp.unsafeSend
  }

  override def onBackPressed(): Unit = unsafeSend(Back)

  def onBackPressedNative(): Unit = super.onBackPressed()
}

object StateActivity
{
  implicit def ToAIORunner_StateActivity: ToAIORunner[StateActivity] =
    new ToAIORunner[StateActivity] {
      type R = StateActivityAIO
      def pure(io: AIO[Message, StateActivity]) = StateActivityAIO(io)
    }
}

case class StateActivityAIO(io: AIO[Message, StateActivity])
extends AIORunner[StateActivity, StateActivityAIO]
{
  def main = new StateActivityAIO(io) { override def runOnMain = true }
}

trait StateActAIO
{
  def stateActAIO[A](f: StateActivity => A): AIO[A, StateActivity] =
    macro view.AnnotatedAIOM.inst[A, StateActivity]
}
