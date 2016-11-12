package tryp
package droid
package state

import scala.annotation.implicitNotFound

import scalaz._, Scalaz._

import scalaz.stream.Process._

import simulacrum._

trait StateEffectInstances
{
//   implicit def instance_StateEffect_IO
//   [F[_, _]: PerformIO, A: Operation, C: IOMessage]
//   (implicit O: Operation[F[A, C]])
//   = new StateEffect[F[A, C]] {
//       def stateEffect(fa: F[A, C]) = O.result(fa).stateEffect
//     }

  implicit def instance_StateEffect_IO[F[_, _]: PerformIO, A, C: IOMessage]
  (implicit se: StateEffect[ZTask[A]])
  : StateEffect[F[A, C]] =
    new StateEffect[F[A, C]] {
      def stateEffect(v: F[A, C]) = {
        tryp.state.Effect(Process.emit(IOTask(v, v.toString).publish.success),
          "IO")
      }

      override def toString = "StateEffect[ViewStream]"
    }

  implicit def instance_StateEffect_IO_Unit[F[_, _]: PerformIO, C: IOMessage]
  (implicit O: Operation[F[Unit, C]]) =
    new StateEffect[F[Unit, C]] {
      def stateEffect(fa: F[Unit, C]) = O.result(fa).stateEffect
    }
}
