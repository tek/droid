package tryp
package droid
package integration

import android.support.v7.app.AppCompatActivity

import shapeless._

import fs2.async

import iota._

import tryp.state.annotation._
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
extends StateActivity
{
  override def onCreate(state: Bundle) = {
    super.onCreate(state)
    // stateApp.send(Msg)
  }
}

case class IntMain(container: FrameLayout, tv: TextView)
extends ViewTree[FrameLayout]
{
  tv.setText("success")
  tv.setTextSize(50)

  override def toString = "IntMain"
}

@machine
object IntView
extends ViewMachine[IntMain]
{
  def infMain = inflate[IntMain]

  def trans: Transitions = {
    case Msg => HNil
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
    val (agent, state) = Agent.pristine(android.aux :: MVFrame.aux :: IntView.aux :: HNil)
    Loop.ags(agent :: HNil, state :: HNil)
  }
}

class IntApplication
extends StateApplication
{
  lazy val state = new IntAppState
}
