package tryp
package droid

import java.util.concurrent._

import Z._

object TaskOps
{
  def infraResult[A](desc: String)(res: Throwable \/ A)
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

object ShortPool
extends FixedPool
{
  val threads = 3
}

final class TaskOps[A](task: Task[A])(implicit log: Logger)
{
  def infraRun(desc: String) = {
     TaskOps.infraResult(desc)(task.unsafePerformSyncAttempt)
  }

  def infraRunFor(desc: String, timeout: Duration)
  (implicit x: ExecutorService) = {
     val res = Task.fork(task)(x)
       .unsafePerformSyncAttemptFor(timeout)
     TaskOps.infraResult(desc)(res)
  }

  def infraRunShort(desc: String)(implicit timeout: Duration = 5 seconds) = {
    implicit val exec = ShortPool.executor
    infraRunFor(desc, timeout)
  }

  def !?(desc: String)(implicit timeout: Duration = 5 seconds) =
    infraRunShort(desc)

  def fork(f: (Throwable \/ A) ⇒ Unit)(implicit x: ExecutorService) = {
    Task.fork(task)(x).unsafePerformAsync(f)
  }

  def infraFork(desc: String)(implicit x: ExecutorService) = {
    task.unsafePerformAsync(TaskOps.infraResult[A](desc) _)
  }

  def infraForkShort(desc: String)
  (implicit x: ExecutorService, timeout: Duration = 5 seconds) = {
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
