package tryp
package droid

import concurrent.duration._

import scalaz.concurrent.Task

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

  def infraRunShort(desc: String)(implicit log: Logger) = {
     TaskOps.infraResult(desc)(task.attemptRunFor(5 seconds))
  }

  def !?(desc: String)(implicit log: Logger) = infraRunShort(desc)

  def infraRunAsync(desc: String)(implicit log: Logger) = {
    task.runAsync(TaskOps.infraResult[A](desc) _)
  }

  def infraRunAsyncShort(desc: String)(implicit log: Logger) = {
    task.timed(5 seconds).runAsync(TaskOps.infraResult[A](desc) _)
  }
}

trait ToTaskOps
{
  implicit def ToTaskOps[A](task: Task[A])(implicit log: Logger) =
    new TaskOps(task)
}
