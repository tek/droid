package tryp
package droid

import concurrent.duration._

import scalaz.stream.Process
import scalaz.concurrent.Task

import org.log4s.Logger

final class TaskProcessOps[A](proc: Process[Task, A])
extends ToTaskOps
{
  def infraRunAsync(desc: String)(implicit log: Logger) = {
    proc.run.infraRunAsync(desc)
  }

  def infraRunFor(desc: String, timeout: Duration)(implicit log: Logger) = {
    proc.run.infraRunFor(desc, timeout)
  }
}

trait ToProcessOps
{
  implicit def ToTaskProcessOps[A](task: Process[Task, A])
  (implicit log: Logger) =
    new TaskProcessOps(task)
}
