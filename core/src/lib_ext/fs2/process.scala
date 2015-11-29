package tryp
package droid

import concurrent.duration._

import scalaz.stream.Process
import scalaz.concurrent.Task

import org.log4s.Logger

final class TaskProcessOps[A](proc: Process[Task, A])(implicit log: Logger)
extends ToTaskOps
{
  def infraRunAsync(desc: String) = {
    proc.run.infraRunAsync(desc)
  }

  def infraRunFor(desc: String, timeout: Duration) = {
    proc.run.infraRunFor(desc, timeout)
  }

  def infraRunShort(desc: String)(implicit timeout: Duration = 5 seconds) = {
    proc.run.infraRunShort(desc)
  }

  def infraRunLogAsync(desc: String) = {
    proc.runLog.infraRunAsync(desc)
  }

  def infraRunLogFor(desc: String, timeout: Duration) = {
    proc.runLog.infraRunFor(desc, timeout)
  }

  def peek()(implicit timeout: Duration = 5 seconds) = proc.runLog.peek()
}

trait ToProcessOps
{
  implicit def ToTaskProcessOps[A](task: Process[Task, A])
  (implicit log: Logger) =
    new TaskProcessOps(task)
}
