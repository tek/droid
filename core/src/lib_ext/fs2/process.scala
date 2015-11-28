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

  def infraRunShort(desc: String)(implicit log: Logger) = {
    proc.run.infraRunShort(desc)
  }

  def infraRunLogAsync(desc: String)(implicit log: Logger) = {
    proc.runLog.infraRunAsync(desc)
  }

  def infraRunLogFor(desc: String, timeout: Duration)(implicit log: Logger) = {
    proc.runLog.infraRunFor(desc, timeout)
  }

  def peek() = proc.runLog.peek()
}

trait ToProcessOps
{
  implicit def ToTaskProcessOps[A](task: Process[Task, A]) =
    new TaskProcessOps(task)
}
