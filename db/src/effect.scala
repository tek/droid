package tryp
package droid
package db

import scala.concurrent.Await

import droid.state.FromContextAIO
import FromContextDb._

trait DbStateEffectInstances
{
  implicit def instance_StateEffect_AnyAction[A: Operation, E <: SlickEffect] =
    new StateEffect[SlickAction[A, E]] {
      def stateEffect(fa: SlickAction[A, E]) =
        DbTask[A, E](fa).publish.success.stateEffect
    }
}

case class DbTask[A: Operation, E <: SlickEffect](action: SlickAction[A, E])
extends Message
{
  def task(dbi: DbInfo): Effect = {
    IO.delay(Await.result(dbi.db() run(action), 5.seconds)).stateEffect
  }

  def effect(dbi: Option[DbInfo]): Effect = {
    dbi.map(task) | {
      val io = AIO((dbi: DbInfo) => task(dbi))
      val fc = FromContextAIO[AIO, Effect, DbInfo](io)
      fc.effect
    }
  }
}
