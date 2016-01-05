package tryp
package droid

import Z._

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

final class TaskOps[A](task: Task[A])(implicit log: Logger)
{
  def infraRun(desc: String) = {
     TaskOps.infraResult(desc)(task.unsafePerformSyncAttempt)
  }

  def infraRunFor(desc: String, timeout: Duration) = {
     TaskOps.infraResult(desc)(task.unsafePerformSyncAttemptFor(timeout))
  }

  def infraRunShort(desc: String)(implicit timeout: Duration = 5 seconds) = {
    infraRunFor(desc, timeout)
  }

  def !?(desc: String) = infraRunShort(desc)

  def infraRunAsync(desc: String) = {
    task.unsafePerformAsync(TaskOps.infraResult[A](desc) _)
  }

  def infraRunAsyncShort(desc: String)
  (implicit timeout: Duration = 5 seconds) = {
    task
      .unsafePerformTimed(timeout)
      .unsafePerformAsync(TaskOps.infraResult[A](desc) _)
  }

  def peek() = task.unsafePerformSyncAttemptFor(5 seconds)
}

trait ToTaskOps
{
  implicit def ToTaskOps[A](task: Task[A])(implicit log: Logger) =
    new TaskOps(task)
}
