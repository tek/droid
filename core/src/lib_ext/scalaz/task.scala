package tryp
package droid

import concurrent.duration._

import Z._

import org.log4s.Logger

object TaskOps
{
  def infraResult[A](desc: String)(res: \/[Throwable, A])
  (implicit log: Logger): Maybe[A] = {
    res match {
      case -\/(e) ⇒
        log.error(s"failed to $desc: $e")
        Maybe.empty[A]
      case a ⇒
        a.toMaybe
    }
  }
}

final class TaskOps[A](task: Task[A])
{
  def infraRun(desc: String)(implicit log: Logger) = {
     TaskOps.infraResult(desc)(task.attemptRun)
  }

  def infraRunFor(desc: String, timeout: Duration)(implicit log: Logger) = {
     TaskOps.infraResult(desc)(task.attemptRunFor(timeout))
  }

  def infraRunShort(desc: String)(implicit log: Logger) = {
    infraRunFor(desc, 5 seconds)
  }

  def !?(desc: String)(implicit log: Logger) = infraRunShort(desc)

  def infraRunAsync(desc: String)(implicit log: Logger) = {
    task.runAsync(TaskOps.infraResult[A](desc) _)
  }

  def infraRunAsyncShort(desc: String)(implicit log: Logger) = {
    task.timed(5 seconds).runAsync(TaskOps.infraResult[A](desc) _)
  }

  def peek() = task.attemptRunFor(5 seconds)
}

trait ToTaskOps
{
  implicit def ToTaskOps[A](task: Task[A]) = new TaskOps(task)
}
