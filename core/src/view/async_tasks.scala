package tryp.droid

import scalaz._, Scalaz._, concurrent._

abstract class AsyncTasks
extends StatefulFragment
{
  import ViewState._

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
    override def toString = s"async task finished with '$msg'"
  }

  case class FabData(running: Seq[Task[_]])
  extends Data

  case object Idle
  extends BasicState

  case object Running
  extends BasicState

  override def impls = fabImpl :: super.impls

  val fabImpl = new StateImpl
  {
    override def description = "fab state"

    private[this] def execTask(msg: AsyncTask, data: FabData, fade: Boolean) =
    {
      val optFade = if (fade) switchToAsyncUi.some else none
      val t = Task {
        msg.task.attemptRun
          .fold(f ⇒ AsyncTaskFailure(f, msg.failure),
            s ⇒ AsyncTaskSuccess(s, msg.success))
          .successNel[Message]
      }
      S(Running, FabData(data.running :+ msg.task)) << optFade << t << msg.done
    }

    def startTask(msg: AsyncTask): ViewTransition = {
      case S(Idle, f @ FabData(_)) ⇒
        execTask(msg, f, true)
      case S(Running, f @ FabData(_)) ⇒
        execTask(msg, f, false)
    }

    def taskDone(msg: AsyncTaskDone): ViewTransition = {
      case S(Running, FabData(tasks)) ⇒
        val remaining = tasks filterNot(_ == msg.task)
        if (remaining.isEmpty)
          S(Idle, FabData(Nil)) << switchToIdleUi
        else
          S(Running, FabData(remaining))
    }

    def taskSuccess(msg: AsyncTaskSuccess): ViewTransition = {
      case s ⇒ s << AsyncTaskResult(msg.result) << msg.toast.map(Toast(_))
    }

    def taskFail(msg: AsyncTaskFailure): ViewTransition = {
      case s ⇒ s << LogFatal("executing async task", msg.error) <<
        msg.toast.map(Toast(_))
    }

    val create: ViewTransition = {
      case S(Pristine, _) ⇒ S(Idle, FabData(Nil))
    }

    val transitions: ViewTransitions = {
      case Create(_, _) ⇒ create
      case m: AsyncTask ⇒ startTask(m)
      case m: AsyncTaskDone ⇒ taskDone(m)
      case m: AsyncTaskSuccess ⇒ taskSuccess(m)
      case m: AsyncTaskFailure ⇒ taskFail(m)
    }
  }

  def switchToAsyncUi: AppEffect = Ui.nop

  def switchToIdleUi: AppEffect = Ui.nop
}
