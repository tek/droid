package tryp
package droid

import scalaz._, Scalaz._, concurrent._, stream._, Process._

import com.google.android.gms
import gms.common.{ConnectionResult, GooglePlayServicesUtil}
import gms.common.{api ⇒ gapi}
import gapi.GoogleApiClient


import State._

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
extends DroidStateBase[A]
{
  runFsm(Disconnected)

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

  def apiConnected(data: Bundle) = {
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

  val basicTransitions: ViewTransitions = {
    case Connect ⇒ connect
    case ConnectionEstablished ⇒ connectionEstablished
    case ConnectionLost ⇒ connectionLost
    case ConnectionFailed(_) ⇒ connectionLost
    case Disconnect ⇒ disconnect
  }

  def transitions = basicTransitions

  def connect: ViewTransition = {
    case S(Disconnected, d) ⇒
      S(Connecting, d) <<
        stateEffect("connecting play services") {
          apiClient foreach(_.connect())
        }
  }

  def disconnect: ViewTransition = {
    case S(_, d) ⇒
      S(Disconnected, d) <<
        stateEffect("disconnecting play services") {
          apiClient foreach(_.disconnect())
        }
  }

  def connectionEstablished: ViewTransition = {
    case S(_, d) ⇒
      S(Connected, d) << isConnected.set(true).effect
  }

  def connectionLost: ViewTransition = {
    case S(_, d) ⇒
      S(Disconnected, d)
  }
}
