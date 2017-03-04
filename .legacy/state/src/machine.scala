package tryp
package droid
package state

import tryp.state._

import cats._
import cats.instances.all._

case class MachineTerminated(z: Machine)
extends Message

trait IOTrans
extends MachineTransitions
with view.AnnotatedIO
{
  def instance_MOutput_IOTask[F[_, _]: PerformIO, A: Operation, C]
  : MOutput[IOTask[F, A, C]] = new MOutput[IOTask[F, A, C]] {
    def broadcast = true
  }

  def instance_MOutput_IOFun[A, C]
  : MOutput[IOFun[A, C]] = new MOutput[IOFun[A, C]] {
    def broadcast = true
  }

  def actAs[A <: Activity: ClassTag, B: Operation](f: A => B)
  : IOI[Effect, Activity] = act { a =>
    a match {
      case aa: A => fs2.Task.delay(f(aa)).stateEffect
      case _ =>
        LogError("creating activity IO",
          s"Can't run '${className[A]}' task with current '${a.className}'")
            .stateEffect
    }
  }
}
