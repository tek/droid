package tryp
package droid

import state.core.{Create, Resume}

trait FreeActivityAgent
extends Activity
with RootAgent
{
  implicit def activity = this

  override def handle = "activity"

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    forkAgent()
    send(Create(Map(), Option(saved)))
  }

  override def onResume() {
    super.onResume()
    send(Resume)
  }

  override def machines = ioMachine %:: activityMachine %:: super.machines

  protected lazy val activityMachine = new Machine
  {
    override def description = "activity access state"

    override def handle = "activity"

    def toast(id: String): Transit = {
      case s => s
        // s << uiCtx.notify(id)
    }

    val admit: Admission = {
      case state.core.Toast(id) => toast(id)
    }
  }

  lazy val ioMachine = new state.IODispatcher {}
}
