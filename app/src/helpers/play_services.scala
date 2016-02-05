package tryp
package droid

import scalaz._, Scalaz._, stream._, Process._

import com.google.android.gms
import gms.common.{ConnectionResult, GooglePlayServicesUtil}
import gms.common.{api ⇒ gapi}
import gapi.GoogleApiClient

import state._

object PlayServices
{
  case object Connect
  extends Message

  case object Disconnect
  extends Message

  case object ConnectionEstablished
  extends Message

  case object ConnectionLost
  extends Message

  case class ConnectionFailed(result: ConnectionResult)
  extends Message

  case object Connected
  extends BasicState

  case object Disconnected
  extends BasicState

  case object Connecting
  extends BasicState
}
import PlayServices._

trait PlayServices[A <: WithContext]
extends DroidMachine[A]
{
  fork(initial = Disconnected)

  class ConnectionCallbacks(owner: PlayServices[A])
  extends GoogleApiClient.OnConnectionFailedListener
  with GoogleApiClient.ConnectionCallbacks
  {
    def onConnected(data: Bundle) {
      Log.i("Google Api connected")
      owner.apiConnected(data)
    }

    def onConnectionFailed(connectionResult: ConnectionResult) {
      if (!connectionResult.isSuccess) {
        val err = connectionResult.getErrorCode
        Log.e(s"Google Api connection failed: $err")
        owner.apiConnectionFailed(connectionResult)
      }
    }

    def onConnectionSuspended(cause: Int) {
      owner.send(ConnectionLost)
      Log.e("Google Api connection suspended")
    }
  }

  def handle = s"playservices.$subHandle"

  def subHandle: String

  def playServicesAvailable = {
    ctx.contextO
      .map(GooglePlayServicesUtil.isGooglePlayServicesAvailable)
      .contains(1)
  }

  private lazy val connectionCallbacks = new ConnectionCallbacks(this)

  protected lazy val apiClient = builder.map(_.build)

  def builder = {
    ctx context { context ⇒
      new GoogleApiClient.Builder(context)
        .addApi(api)
        .addConnectionCallbacks(connectionCallbacks)
        .addOnConnectionFailedListener(connectionCallbacks)
    }
  }

  protected def api: gapi.Api[_ <: gapi.Api.ApiOptions.NotRequiredOptions]

  def apiConnectionFailed(result: ConnectionResult) = {
    send(ConnectionFailed(result))
  }

  def apiConnected(data: Bundle): Unit = {
    send(ConnectionEstablished)
  }

  lazy val isConnected = async.signalOf(false)

  lazy val client = {
    apiClient
      .map(Process.emit)
      .some(_.repeat.when(isConnected.discrete))
      .none(Process.halt)
  }

  def oneClient = {
    send(Connect)
    client |> await1
  }

  val basicAdmit: Admission = {
    case Connect ⇒ connect
    case ConnectionEstablished ⇒ connectionEstablished
    case ConnectionLost ⇒ connectionLost
    case ConnectionFailed(_) ⇒ connectionLost
    case Disconnect ⇒ disconnect
  }

  def admit = basicAdmit

  def connect: Transit = {
    case S(Disconnected, d) ⇒
      S(Connecting, d) <<
        stateEffect("connecting play services") {
          apiClient foreach(_.connect())
        }
  }

  def disconnect: Transit = {
    case S(_, d) ⇒
      S(Disconnected, d) <<
        stateEffect("disconnecting play services") {
          apiClient foreach(_.disconnect())
        }
  }

  def connectionEstablished: Transit = {
    case S(_, d) ⇒
      S(Connected, d) << isConnected.set(true).effect("set signal isConnected")
  }

  def connectionLost: Transit = {
    case S(_, d) ⇒
      S(Disconnected, d)
  }
}
