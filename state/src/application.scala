package tryp
package droid
package state

import android.support.v7.app.AppCompatActivity

import shapeless._, ops.hlist._

import fs2.async

import tryp.state.annotation._

case class IOMState(act: Activity)
extends CState

@cell
class AndroidCell
(implicit sched: Scheduler)
extends AnnotatedTIO
{
  def trans: Transitions = {
    case SetActivity(act) =>
      IOMState(act) :: HNil
    case m: ContextIO => {
      case IOMState(act) => TaskIO(m.task(act), m.desc) :: HNil
    }
    case m: ActivityIO => {
      case IOMState(act) => TaskIO(m.task(act), m.desc) :: HNil
    }
    case m: AppCompatActivityIO => {
      case IOMState(act: AppCompatActivity) => TaskIO(m.task(act), m.desc) :: HNil
    }
    case SetContentTree(t) => act(_.setContentView(t.container)) :: HNil
  }
}

trait AppState
extends Logging
{
  implicit val pool: SchedulerStrategyPool
  import pool._

  def loopCtor: Task[(Loop.MQueue, Signal[Boolean], Loop.OStream)]

  def ctor = {
    for {
      ito <- loopCtor
      (in, term, out) = ito
      loop <- Task.start(out.runLog)
    } yield (in, term, loop)
  }

  lazy val (in, term, loop) = ctor.unsafeRun()

  def run() = loop.unsafeRunAsync { case a => log.info(s"finished with $a") }

  def send(msg: Message) = in.publish1(msg).unsafeRun()

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
extends AppCompatActivity
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
