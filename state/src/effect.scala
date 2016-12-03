package tryp
package droid
package state

import scala.annotation.implicitNotFound

import simulacrum._

trait StateEffectInstances
{
  implicit def instance_StateEffect_IO
  [F[_, _]: PerformIO: DescribeIO, A: StateEffect, C: IOMessage]
  : StateEffect[F[A, C]] =
    new StateEffect[F[A, C]] {
      def stateEffect(v: F[A, C]) = {
        Effect.now(IOTask(v, v.desc).publish, "IO")
      }

      override def toString = "StateEffect[ViewStream]"
    }

  implicit def instance_StateEffect_IO_Unit[F[_, _]: PerformIO, C: IOMessage]
  (implicit O: Operation[F[Unit, C]]) =
    new StateEffect[F[Unit, C]] {
      def stateEffect(fa: F[Unit, C]) = O.result(fa).stateEffect
    }
}
