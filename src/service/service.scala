package tryp.droid.service

import android.app.Service
import android.os.{Binder,IBinder,Bundle}
import android.content.{Intent,Context}

import scala.reflect.ClassTag
import scala.reflect.classTag

trait ServiceStubs
{
  def onStart = ()
  def onStop = ()
  def onPause = ()
  def onResume = ()
}

abstract class ServiceBase
extends Service
with ServiceStubs
with tryp.droid.Basic
with tryp.droid.Broadcast
{
  var running = false

  class TrypBinder extends Binder {
    def getService() = ServiceBase.this
  }

  override def onStartCommand(intent: Intent, flags: Int, startId: Int): Int =
  {
    start
    thread(handleIntent(intent))
    Service.START_STICKY
  }

  override def onBind(intent: Intent): IBinder = {
    start
    new TrypBinder
  }

  private def start {
    if (!running) {
      running = true
      init
      onStart
    }
  }

  override def onDestroy {
    onStop
  }

  override def onCreate(state: Bundle) = ()
  override def onStart = super.onStart
  override def onStop = super.onStop
  override def onResume = super.onResume
  override def onPause = super.onPause

  override implicit def context = this
  def init
  def handleIntent(intent: Intent)
}

class ServiceFactory[A <: Service : ClassTag]
{
  val ServiceClass = classTag[A].runtimeClass

  def start(implicit context: Context) = {
    context.startService(new Intent(context, ServiceClass))
  }
}
