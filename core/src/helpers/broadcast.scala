package tryp.droid

import scala.collection.mutable.ListBuffer

import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager

class BroadcastReceiver(client: BroadcastReceive)
extends android.content.BroadcastReceiver
{
  def onReceive(context: Context, intent: Intent) {
    client.broadcastReceived(intent)
  }
}

trait BroadcastBase
{
  def context: Context

  protected def broadcastManager = LocalBroadcastManager.getInstance(context)
}

trait BroadcastReceive
extends BroadcastBase
with CallbackMixin
{
  val broadcastEvents = ListBuffer[String]()

  protected def registerEvents(names: String*) {
    broadcastEvents ++= names
  }

  abstract override def onStart() = {
    super.onStart()
    listen(broadcastEvents:_*)
  }

  abstract override def onStop() = {
    super.onStop
    unregister
  }

  protected lazy val receiver = new BroadcastReceiver(this)

  protected def listen(events: String*) {
    events.foreach(register(_))
  }

  private def register(event: String) {
    broadcastManager.registerReceiver(receiver, new IntentFilter(event))
  }

  private def unregister = broadcastManager.unregisterReceiver(receiver)

  def handleBroadcast(action: String, extras: Params) = {
  }

  def broadcastReceived(intent: Intent) {
    val extras = Option(intent.getExtras) some(_.toParams) none(Params())
    handleBroadcast(intent.getAction, extras)
  }
}

trait BroadcastSend
extends BroadcastBase
{
  protected def broadcast(message: String, payload: Params = null)
  {
    val intent = new Intent(message)
    if (payload != null) {
      for((k, v) <- payload.params) { intent.putExtra(k, v) }
    }
    broadcastManager.sendBroadcast(intent)
  }
}

trait Broadcast
extends BroadcastReceive
with BroadcastSend
