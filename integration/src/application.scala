package tryp
package droid
package integration

import shapeless._

import fs2.async

import tryp.state.annotation._

import state._
import tryp.state.core.{Loop, Exit}

case object Msg
extends Message

case class SetActivity(act: Activity)
extends Message

case class IOMState(act: Activity)
extends MState

case object Dummy
extends Message

case class MessageTask(task: Task[Message])

@machine
object IOMac
extends AnnotatedTIO
{
  implicit def sched = DefaultScheduler.scheduler

  def tv(c: Context) = {
    val t = new TextView(c)
    t.setText("success")
    t.setTextSize(50)
    t
  }

  def trans: Transitions = {
    case Msg =>
      act(a => a.setContentView(tv(a))).main :: HNil
    case SetActivity(act) =>
      IOMState(act) :: HNil
    case m: ActivityIO => {
      case IOMState(act) => StateIO(MessageTask(m.task(act))) :: HNil
    }
  }
}

object StatePool
extends ExecutionStrategyPool
with Logging
{
  def name = "state"

  def hook(t: Thread) = {
    t
  }

  implicit lazy val executor = BoundedCachedExecutor.withHook(name, 1, 10, 100, hook)
}

object DefaultScheduler
{
  implicit lazy val scheduler: Scheduler = Scheduler.fromFixedDaemonPool(1)
}

class IntStateActivity
extends Activity
{
  lazy val stateApp = getApplication match {
    case a: IntApplication => a
    case _ => sys.error("application is not a StateApplication")
  }

  override def onCreate(state: Bundle) = {
    stateApp.setActivity(this)
    stateApp.send(Msg)
    super.onCreate(state)
  }
}

class IntApplication
extends android.app.Application
with ExecutionStrategy
{
  implicit def scheduler = DefaultScheduler.scheduler

  def pool = StatePool

  def stateIO(io: StateIO): Stream[Task, Message] = {
    io match {
      case StateIO(MessageTask(t)) => Stream.eval(t)
      case StateIO(t: Task[_]) => Stream.eval(t).drain
      case _ => Stream()
    }
  }

  def result(in: Queue[Message], state: ExecuteTransition.StateI): Stream[Task, ExecuteTransition.StateI] = {
    Stream.emits(state.ios).flatMap(stateIO).to(in.enqueue).drain ++ Stream(state)
  }

  def ctor = {
    val (agent, state) = Agent.pristine(IOMac.aux :: HNil)
    val io = Loop.ags(agent :: HNil, state :: HNil)
    for {
      term <- async.signalOf[Task, Boolean](false)
      io1 <- io
      (in, out) = io1
      loop <- Task.start(term.interrupt(out.flatMap(result(in, _))).runLog)
    } yield (term, in, loop)
  }

  lazy val (term, in, loop) = ctor.unsafeRun()

  override def onCreate(): Unit = {
    loop.unsafeRunAsync { case a => dbg(s"finished with $a") }
    super.onCreate()
  }

  def send(msg: Message) = in.enqueue1(msg).unsafeRun()

  def setActivity = SetActivity.apply _ andThen send _
}
