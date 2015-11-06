package tryp.droid

import android.app.Service
import android.os.{Binder,IBinder}

import scala.reflect.classTag

trait ServiceStubs
{
  def onStart() = ()
  def onStop() = ()
  def onPause() = ()
  def onResume() = ()
}

trait ServiceCommon
extends HasContext
{
  implicit val comm = DummyCommunicator()

  implicit def ec = scala.concurrent.ExecutionContext.Implicits.global
}

abstract class ServiceBase
extends Service
with ServiceStubs
with Basic
with ServiceCommon
{
  var running = false

  def context = this

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

  override def onCreate() = ()
  override def onStart = super.onStart
  override def onStop() = super.onStop()
  override def onResume = super.onResume
  override def onPause = super.onPause

  def init
  def handleIntent(intent: Intent)
}

class ServiceFactory[A <: Service: ClassTag]
{
  val ServiceClass = classTag[A].runtimeClass

  def start(implicit context: Context) = {
    context.startService(new Intent(context, ServiceClass))
  }
}
