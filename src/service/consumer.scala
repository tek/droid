package tryp.droid.service

import scala.collection.mutable.{Map,ListBuffer}

import android.app.Service
import android.content.{ServiceConnection,ComponentName,Context,Intent}
import android.os.IBinder

import tryp.droid.util.CallbackMixin

trait ServiceConsumer extends CallbackMixin {
  // abstract: called when the service has been successfully connected
  protected def onConnectService(name: String)

  val registeredServices = Map[String, Class[_ <: ServiceBase]]()
  val boundServices = Map[String, ServiceBase]()
  val connections = ListBuffer[BindConnection]()

  protected class BindConnection(consumer: ServiceConsumer, context: Context,
    name: String)
  extends ServiceConnection
  {
    override def onServiceConnected(className: ComponentName, binder:
      IBinder) {
        binder match {
          case b: ServiceBase#TrypBinder => {
            boundServices(name) = b.getService
            consumer.onConnectService(name)
          }
          case _  => {
            Log.e("Received invalid binder in onServiceConnected!")
          }
        }
    }

    override def onServiceDisconnected(className: ComponentName) {
    }

    def bind(service: Class[_ <: ServiceBase]) {
      context.bindService(
        new Intent(context, service),
        this,
        Context.BIND_AUTO_CREATE
      )
    }

    def unbind = context.unbindService(this)
  }

  def registerService(cls: Class[_ <: ServiceBase], name: String = "service") {
    registeredServices(name) = cls
  }

  def service(name: String = "service"): ServiceBase = boundServices(name)

  abstract override def onStart {
    super.onStart
    for ((name, cls) <- registeredServices) {
      new BindConnection(this, context, name) +=: connections
      connections.head.bind(cls)
    }
  }

  abstract override def onStop {
    super.onStop
    connections foreach (_.unbind)
    connections.clear
  }
}
