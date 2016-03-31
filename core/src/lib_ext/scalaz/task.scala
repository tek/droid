package tryp
package droid
package core

import java.util.concurrent._

import Z._

object TaskOps
{
  def infraResult[A](desc: String)(res: Throwable \/ A)
  (implicit log: Logger, ex: ExecutorService): Maybe[A] = {
    ex match {
      case _: PoolExecutor =>
      case e =>
        sys.error(s"invalid executor $e")
    }
    res match {
      case -\/(t: TimeoutException) =>
        val threads = sys.allThreads.length
        val e = s"timed out trying to $desc, executor: $ex, threads: $threads"
        log.error(e)
        PoolExecutor.logInfo()
        Maybe.empty[A]
      case -\/(e) =>
        log.error(s"failed to $desc: $e")
        Maybe.empty[A]
      case a =>
        a.toMaybe
    }
  }
}

object ShortPool
extends FixedPool
{
  def name = "short tasks"

  val threads = 3
}

object TimedPool
extends BoundedCachedPool
{
  def name = "timed tasks"
}

object SyncPool
extends CachedPool
{
  def name = "sync"
}

final class TaskOps[A](task: Task[A])
{
  def runDefault() = {
    Task.fork(task)(SyncPool.executor).unsafePerformSyncAttempt
  }

  def fork(f: (Throwable \/ A) => Unit)(implicit x: ExecutorService) = {
    Task.fork(task)(x).unsafePerformAsync(f)
  }
}

trait ToTaskOps
{
  implicit def ToTaskOps[A](task: Task[A]) =
    new TaskOps(task)
}

final class InfraTaskOps[A](task: Task[A])(implicit log: Logger)
extends ToTaskOps
{
  def infraRun(desc: String) = {
     implicit val e = SyncPool.executor
     TaskOps.infraResult(desc)(Task.fork(task)(e).unsafePerformSyncAttempt)
  }

  def infraRunFor(desc: String, timeout: Duration)
  (implicit ex: ExecutorService) = {
     // implicit val ex = TimedPool.executor
     val res = Task.fork(task)(ex)
       .unsafePerformSyncAttemptFor(timeout)
     TaskOps.infraResult(desc)(res)
  }

  def infraRunShort(desc: String)(implicit timeout: Duration = 5 seconds) = {
    implicit val ex = ShortPool.executor
    infraRunFor(desc, timeout)(ex)
  }

  def !?(desc: String)(implicit timeout: Duration = 5 seconds) =
    infraRunShort(desc)

  def infraFork(desc: String)(implicit x: ExecutorService) = {
    val f: Throwable \/ A => Unit =
      TaskOps.infraResult[A](desc)(_)
        .map(res => log.debug(s"forked process '$desc' completed with $res"))
    task.fork(f)(x)
  }

  def infraForkShort(desc: String)
  (implicit x: ExecutorService, timeout: Duration = 5 seconds) = {
    new InfraTaskOps(task.unsafePerformTimed(timeout)).infraFork(desc)(x)
  }

  def peek() = task.unsafePerformSyncAttemptFor(5 seconds)
}

trait ToInfraTaskOps
{
  implicit def ToInfraTaskOps[A](task: Task[A])(implicit log: Logger) =
    new InfraTaskOps(task)
}
