package tryp.droid

import android.content.Context
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.app.TaskStackBuilder
import android.content.Intent
import android.app.PendingIntent

class Notification(
  context: Context, target: Class[_], icon: Int, title: String, id: Int
)
{
  var text = ""

  lazy val builder = new NotificationCompat.Builder(context) tap { bldr ⇒
    val sb = TaskStackBuilder.create(context)
    sb.addParentStack(target)
    sb.addNextIntent(new Intent(context, target))
    val intent = sb.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
    bldr.setContentIntent(intent)
    bldr.setContentTitle(title)
    bldr.setSmallIcon(icon)
  }

  def setTitle(newTitle: String) = builder.setContentTitle(newTitle)

  def setText(newText: String) = {
    text = newText
    builder.setContentText(newText)
  }

  def setIcon(newIcon: Int) = builder.setSmallIcon(newIcon)

  def setProgress(values: (Int, Int)) {
    builder.setProgress(values._2, values._1, false)
  }

  def update = manager.notify(id, builder.build)

  def cancel = manager.cancel(id)

  private def manager: NotificationManagerCompat = {
    NotificationManagerCompat.from(context)
  }
}

trait Notifications
extends Basic
{
  def createNotification(
    target: Class[_], icon: Int, title: String, id: Int = 1
  ) = {
    new Notification(context, target, icon, title, id)
  }
}
