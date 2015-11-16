package tryp
package droid

import scalaz._, Scalaz._, concurrent._

import org.log4s.Logger

object TaskOps
{
  def infraResult[A](desc: String)(res: \/[Throwable, A])
  (implicit log: Logger) = {
    res match {
      case -\/(e) ⇒ log.error(s"failed to $desc: $e")
      case _ ⇒
    }
  }
}

final class TaskOps[A](task: Task[A])
{
  def infraRun(desc: String)(implicit log: Logger) = {
     TaskOps.infraResult(desc)(task.attemptRun)
  }
}

trait ToTaskOps
{
  implicit def ToTaskOps[A](task: Task[A])(implicit log: Logger) =
    new TaskOps(task)
}