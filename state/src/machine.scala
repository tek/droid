package tryp
package droid
package state

import tryp.state._

import scalaz.stream
import stream.async

import scalaz.syntax.std.option._
import scalaz.syntax.apply._
import scalaz.syntax.show._

import cats._
import cats.instances.all._

case class MachineTerminated(z: Machine)
extends Message

trait Machine
extends tryp.state.Machine
with view.AnnotatedIO
{
  def instance_PublishFilter_IOTask[F[_, _]: PerformIO, A: Operation, C]
  : PublishFilter[IOTask[F, A, C]] = new PublishFilter[IOTask[F, A, C]] {
    def allowed = true
  }

  def instance_PublishFilter_IOFun[A, C]
  : PublishFilter[IOFun[A, C]] = new PublishFilter[IOFun[A, C]] {
    def allowed = true
  }

  def actAs[A <: Activity: ClassTag, B: Operation](f: A => B)
  : IO[Effect, Activity] = act { a =>
    a match {
      case aa: A => ZTask(f(aa)).stateEffect
      case _ =>
        LogError("creating activity IO",
          s"Can't run '${className[A]}' task with current '${a.className}'")
            .stateEffect
    }
  }
}
