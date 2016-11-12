package tryp
package droid
package db

import scala.concurrent.Await

import droid.state.FromContextIO
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
    ZTask(Await.result(dbi.db() run(action), Duration.Inf)).stateEffect
  }

  def effect(dbi: Option[DbInfo]): Effect = {
    dbi.map(task) | {
      val io = IO((dbi: DbInfo) => task(dbi))
      val fc = FromContextIO[IO, Effect, DbInfo](io)
      fc.effect
    }
  }
}
