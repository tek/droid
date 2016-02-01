package tryp.droid

import scala.collection.mutable.{Map,ListBuffer}

import android.app.Service
import android.content.{ServiceConnection,ComponentName}
import android.os.IBinder

import ScalazGlobals._

case class ServiceProxy[A <: ServiceBase: ClassTag](
  consumer: ServiceConsumer, name: String
)
{
  def map[B](f: A ⇒ B) = {
    (connected option get) map(f)
  }

  def flatMap[B](f: A ⇒ Option[B]) = {
    (connected option get) flatMap(f)
  }

  def foreach(f: A ⇒ Unit) = {
    (connected option get) foreach(f)
  }

  def get = consumer.service[A](name)

  def connected = consumer.connectedTo(name)
}

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
          case b: ServiceBase#TrypBinder ⇒ {
            boundServices(name) = b.getService
            consumer.onConnectService(name)
          }
          case _  ⇒ {
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
        android.content.Context.BIND_AUTO_CREATE
      )
    }

    def unbind = context.unbindService(this)
  }

  def registerService(cls: Class[_ <: ServiceBase], name: String = "service") {
    registeredServices(name) = cls
  }

  def service[A <: ServiceBase: ClassTag](name: String = "service"): A = {
    boundServices(name) match {
      case s: A ⇒ s
      case s ⇒ {
        throw new ClassCastException(
          s"Wrong service class for ${name}: ${s.getClass.getSimpleName}" +
            s" (wanted ${implicitly[ClassTag[A]].className})"
        )
      }
    }
  }

  def serviceProxy[A <: ServiceBase: ClassTag](name: String = "service") = {
    ServiceProxy[A](this, name)
  }

  def connectedTo(name: String) = boundServices contains name

  abstract override def onStart() {
    super.onStart()
    for ((name, cls) <- registeredServices) {
      new BindConnection(this, context, name) +=: connections
      connections.head.bind(cls)
    }
  }

  abstract override def onStop() {
    super.onStop
    connections foreach (_.unbind)
    connections.clear
  }
}
