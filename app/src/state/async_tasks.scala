package tryp.droid

import scalaz._, Scalaz._, concurrent._

import state._

object AsyncTaskStateData
{
  case class AsyncTask(task: Task[_], success: Option[String],
    failure: Option[String])
  extends Message
  {
    def done = AsyncTaskDone(task)
  }

  case class AsyncTaskDone(task: Task[_])
  extends Message

  case class AsyncTaskSuccess(result: Any, toast: Option[String])
  extends Message

  case class AsyncTaskFailure(error: Throwable, toast: Option[String])
  extends Message

  case class AsyncTaskResult(msg: Any)
  extends Loggable
  {
    def message = s"async task finished with '$msg'"

    override def toString = message
  }

  case class AsyncTasksData(running: Seq[Task[_]])
  extends Data

  case object Idle
  extends BasicState

  case object Running
  extends BasicState
}
import AsyncTaskStateData._

trait AsyncTasksMachine
extends SimpleDroidMachine
{
  private[this] def execTask(msg: AsyncTask, data: AsyncTasksData, fade: Boolean) =
  {
    val optFade = if (fade) switchToAsyncUi.some else none
    val t = Task {
      msg.task.attemptRun
        .fold(f ⇒ AsyncTaskFailure(f, msg.failure),
          s ⇒ AsyncTaskSuccess(s, msg.success))
    }
    S(Running, AsyncTasksData(data.running :+ msg.task)) << optFade << t <<
      msg.done
  }

  def startTask(msg: AsyncTask): Transit = {
    case S(Idle, f @ AsyncTasksData(_)) ⇒
      execTask(msg, f, true)
    case S(Running, f @ AsyncTasksData(_)) ⇒
      execTask(msg, f, false)
  }

  def taskDone(msg: AsyncTaskDone): Transit = {
    case S(Running, AsyncTasksData(tasks)) ⇒
      val remaining = tasks filterNot(_ == msg.task)
      if (remaining.isEmpty)
        S(Idle, AsyncTasksData(Nil)) << switchToIdleUi
      else
        S(Running, AsyncTasksData(remaining))
  }

  def taskSuccess(msg: AsyncTaskSuccess): Transit = {
    case s ⇒ s << AsyncTaskResult(msg.result) << msg.toast.map(Toast(_))
  }

  def taskFail(msg: AsyncTaskFailure): Transit = {
    case s ⇒ s << LogFatal("executing async task", msg.error) <<
      msg.toast.map(Toast(_))
  }

  val create: Transit = {
    case S(Pristine, _) ⇒ S(Idle, AsyncTasksData(Nil))
  }

  val admit: Admission = {
    case Create(_, _) ⇒ create
    case m: AsyncTask ⇒ startTask(m)
    case m: AsyncTaskDone ⇒ taskDone(m)
    case m: AsyncTaskSuccess ⇒ taskSuccess(m)
    case m: AsyncTaskFailure ⇒ taskFail(m)
  }

  def switchToAsyncUi: Effect = Nop

  def switchToIdleUi: Effect = Nop
}
