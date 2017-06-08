package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity

import shapeless._, ops.hlist._

import fs2.async

import tryp.state.annotation._

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

case class IOMState(act: Activity)
extends CState

@cell
object AndroidCell
extends AnnotatedTIO
{
  def trans: Transitions = {
    case SetActivity(act) =>
      IOMState(act) :: HNil
    case m: ContextIO => {
      case IOMState(act) => (strat: Strategy) => TaskIO(m.task(act), m.desc) :: HNil
    }
    case m: ActivityIO => {
      case IOMState(act) => (strat: Strategy) => TaskIO(m.task(act), m.desc) :: HNil
    }
    case m: AppCompatActivityIO => {
      case IOMState(act: AppCompatActivity) => (strat: Strategy) => TaskIO(m.task(act), m.desc) :: HNil
    }
    case SetContentTree(t) => actU(_.setContentView(t.container)) :: HNil
  }
}

trait AppState
extends Logging
{
  def loopCtor: Task[(LoopData.MQueue, Signal[Boolean], LoopData.OStream)]

  def mainView: MVContainer

  def ctor = {
    for {
      ito <- loopCtor
      (in, term, out) = ito
      state <- async.signalOf[Task, List[CState]](Nil)
      sink: fs2.Sink[Task, LoopData.Data] = _.collect{case Left(a) => a}.evalMap(state.set).drain
      loop <- Task.start(out.observe(sink).runLog)
    } yield (in, term, loop, state)
  }

  lazy val (in, term, loop, currentStates) = ctor.unsafeRun()

  def currentState = currentStates.get.unsafeRun()

  def run() = loop.unsafeRunAsync {
    case Left(a: Throwable) =>
      val trace = a.getStackTrace.toList.mkString("\n")
      log.info(s"state loop failed with $a:\n$trace")
    case a => log.info(s"state loop finished with $a")
  }

  def send(msg: Message) = in.enqueue1(msg).unsafeRun()

  // FIXME doesn't work, but doing the same from within the activity does
  def setActivity = SetActivity.apply _ andThen send _
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

  def setActivity = state.setActivity _
}

abstract class StateActivity
extends AppCompatActivity
{
  lazy val stateApp = getApplication match {
    case a: StateApplication => a
    case _ => sys.error("application is not a StateApplication")
  }

  def appState = stateApp.state

  def initialMessages: List[Message]

  override def onCreate(state: Bundle) = {
    Thread.sleep(500)
    stateApp.send(SetActivity(this))
    super.onCreate(state)
    stateApp.send(CreateContentView)
    initialMessages foreach stateApp.send
  }
}
