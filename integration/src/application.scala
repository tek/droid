package tryp
package droid
package integration

import android.support.v7.app.AppCompatActivity

import shapeless._

import fs2.async

import iota._

import tryp.state.annotation._

case object Msg
extends Message

case object Msg2
extends Message

case object Dummy
extends Message

object DefaultScheduler
{
  implicit lazy val scheduler: Scheduler = Scheduler.fromFixedDaemonPool(4)
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
}

case class IntMain(container: FrameLayout, tv: TextView)
extends ViewTree[FrameLayout]
{
  tv.setText("success")
  tv.setTextSize(50)

  override def toString = "IntMain"
}

@cell
object IntView
extends ViewCell[IntMain]
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

  @cell
  object android
  extends AndroidCell

  lazy val loopCtor =
    Loop.cells(android.aux :: ExtMVFrame.aux :: IntView.aux :: HNil, Pristine :: Pristine :: Pristine :: Nil)
}

class IntApplication
extends StateApplication
{
  lazy val state = new IntAppState
}
