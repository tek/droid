package tryp.droid

import scalaz._, Scalaz._, concurrent._

abstract class AsyncTasks
extends StatefulFragment
{
  import ViewEvents._

  case class AsyncTask(task: Task[_], success: Option[String],
    failure: Option[String])
  extends Message
  {
    def done = TaskDone(task)
  }

  case class TaskDone(task: Task[_])
  extends Message

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
          .fold(_ ⇒ Toast(msg.failure), _ ⇒ Toast(msg.success))
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

    def taskDone(msg: TaskDone): ViewTransition = {
      case S(Running, FabData(tasks)) ⇒
        val remaining = tasks filterNot(_ == msg.task)
        if (remaining.isEmpty)
          S(Idle, FabData(Nil)) << switchToIdleUi
        else
          S(Running, FabData(remaining))
    }

    val create: ViewTransition = {
      case S(Pristine, _) ⇒ S(Idle, FabData(Nil))
    }

    val transitions: ViewTransitions = {
      case Create(_, _) ⇒ create
      case m: AsyncTask ⇒ startTask(m)
      case m: TaskDone ⇒ taskDone(m)
    }
  }

  def switchToAsyncUi: Ui[_] = Ui.nop

  def switchToIdleUi: Ui[_] = Ui.nop
}
