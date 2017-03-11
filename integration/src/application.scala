package tryp
package droid
package integration

import android.support.v7.app.AppCompatActivity

import shapeless._

import fs2.async

import tryp.state.annotation._

import state._
import tryp.state.core.{Loop, Exit}

case object Msg
extends Message

case object Msg2
extends Message

case object Dummy
extends Message

object DefaultScheduler
{
  implicit lazy val scheduler: Scheduler = Scheduler.fromFixedDaemonPool(1)
}

object StatePool
extends SchedulerStrategyPool
with Logging
{
  def name = "state"

  def hook(t: Thread) = {
    t
  }

  implicit lazy val executor = BoundedCachedExecutor.withHook(name, 1, 10, 100, hook)

  implicit def scheduler = DefaultScheduler.scheduler
}

class IntStateActivity
extends Activity
{
  lazy val stateApp = getApplication match {
    case a: StateApplication => a
    case _ => sys.error("application is not a StateApplication")
  }

  override def onCreate(state: Bundle) = {
    stateApp.send(SetActivity(this))
    super.onCreate(state)
    stateApp.send(Msg)
  }
}

class IntAppState
extends AppState
{
  implicit val pool = StatePool

  import pool._

  @machine
  object android
  extends AndroidMachine
  with AnnotatedTIO
  {
    def tv(c: Context) = {
      val t = new TextView(c)
      t.setText("success")
      t.setTextSize(50)
      t
    }

    def trans: Transitions = {
      case Msg =>
      dbg("init")
        act { a => a.setContentView(tv(a)); Msg2 }.main :: HNil
      case Msg2 =>
        dbg("success")
        HNil
    }
  }

  lazy val loopCtor = {
    val (agent, state) = Agent.pristine(android.aux :: HNil)
    Loop.ags(agent :: HNil, state :: HNil)
  }

}

class IntApplication
extends StateApplication
{
  lazy val state = new IntAppState
}
