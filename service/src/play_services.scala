package tryp
package droid
package state

import state.core._
import view.core._
import view._

import scalaz.stream._, Process._

import cats._
import cats.syntax.all._
import cats.std.all._

import com.google.android.gms
import gms.common.{ConnectionResult, GooglePlayServicesUtil}
import gms.common.{api => gapi}
import gapi.GoogleApiClient

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

  // implicit def instance_Functor
}
import PlayServices._

trait PlayServices
extends Machine
{
  fork(initial = Zthulhu(state = Disconnected))

  class ConnectionCallbacks(owner: PlayServices)
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
    // con(GooglePlayServicesUtil.isGooglePlayServicesAvailable)
    con(_ => false)
      .map(_ == 1)
  }

  private lazy val connectionCallbacks = new ConnectionCallbacks(this)

  protected lazy val apiClient = {
    builder.map(_.build)
  }

  def builder = {
    con { context =>
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

  lazy val client = emit(apiClient).repeat.when(isConnected.discrete)

  def oneClient = {
    send(Connect)
    client |> await1
  }

  val basicAdmit: Admission = {
    case Connect => connect
    case ConnectionEstablished => connectionEstablished
    case ConnectionLost => connectionLost
    case ConnectionFailed(_) => connectionLost
    case Disconnect => disconnect
  }

  def admit = basicAdmit

  def connect: Transit = {
    case S(Disconnected, d) =>
      S(Connecting, d) << apiClient.map(_.connect())
  }

  def disconnect: Transit = {
    case S(_, d) =>
      S(Disconnected, d) << apiClient.map(_.disconnect())
  }

  def connectionEstablished: Transit = {
    case S(_, d) =>
      S(Connected, d) <<
        isConnected.set(true).stateSideEffect("set signal isConnected")
  }

  def connectionLost: Transit = {
    case S(_, d) =>
      S(Disconnected, d)
  }
}
