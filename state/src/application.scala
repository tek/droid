package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity

import shapeless._, ops.hlist._

import fs2.async

import tryp.state.annotation._

import tryp.state.core.{Loop, Exit, AgsTrans}

case class MessageTask(task: Task[Message], desc: String)

case class IOMState(act: Activity)
extends MState

@machine
class AndroidMachine
(implicit sched: Scheduler)
extends AnnotatedTIO
{
  def trans: Transitions = {
    case SetActivity(act) =>
      dbg("SetActivity")
      IOMState(act) :: HNil
    case m: ContextIO => {
      case IOMState(act) =>
        dbg(s"ContextIO $m")
        StateIO(MessageTask(m.task(act), m.desc)) :: HNil
    }
    case m: ActivityIO => {
      case IOMState(act) =>
        dbg(s"ActivityIO $m")
        StateIO(MessageTask(m.task(act), m.desc)) :: HNil
    }
    case m: AppCompatActivityIO => {
      case IOMState(act: AppCompatActivity) => StateIO(MessageTask(m.task(act), m.desc)) :: HNil
    }
    case SetContentTree(t) => act(_.setContentView(t.container)) :: HNil
  }
}

trait AppState
extends Logging
{
  implicit val pool: SchedulerStrategyPool
  import pool._

  def loopCtor: Task[(Queue[Message], Stream[Task, ExecuteTransition.StateI])]

  def stateIO(io: StateIO): Stream[Task, Message] = {
    dbg(s"io: $io")
    io match {
      case StateIO(MessageTask(t, _)) => Stream.eval(t)
      case StateIO(t: Task[_]) => Stream.eval(t).drain
      case a =>
        log.error(s"invalid IO: $a")
        Stream()
    }
  }

  def result(in: Queue[Message], state: ExecuteTransition.StateI): Stream[Task, ExecuteTransition.StateI] = {
    dbg(s"result: $state")
    Stream.emits(state.ios).flatMap(stateIO).to(in.enqueue).drain ++ Stream(state)
  }

  def ctor = {
    for {
      term <- async.signalOf[Task, Boolean](false)
      io1 <- loopCtor
      (in, out) = io1
      loop <- Task.start(term.interrupt(out.flatMap(result(in, _))).runLog)
    } yield (term, in, loop)
  }

  lazy val (term, in, loop) = ctor.unsafeRun()

  def run() = loop.unsafeRunAsync { case a => log.info(s"finished with $a") }

  def send(msg: Message) = in.enqueue1(msg).unsafeRun()

  // FIXME doesn't work, but doing the same from within the activity does
  def setActivity = SetActivity.apply _ andThen send _
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

class StateActivity
extends Activity
{
  lazy val stateApp = getApplication match {
    case a: StateApplication => a
    case _ => sys.error("application is not a StateApplication")
  }

  override def onCreate(state: Bundle) = {
    stateApp.send(SetActivity(this))
    super.onCreate(state)
    stateApp.send(CreateContentView)
  }
}
