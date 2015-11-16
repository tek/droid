package tryp
package droid

import scalaz._, Scalaz._, concurrent._, stream._, Process._

import com.google.android.gms.common._
import com.google.android.gms.common.api._

import ViewState._

object PlayServices
{
  case object Connect
  extends Message

  case object ConnectionEstablished
  extends Message

  case object ConnectionLost
  extends Message

  case object Connected
  extends BasicState

  case object Disconnected
  extends BasicState

  case object Connecting
  extends BasicState
}
import PlayServices._

trait PlayServices
extends StateImpl
with tryp.droid.Basic
{
  def init() = {
    runFsm(Disconnected)
  }

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

  def handle = "playservices"

  def playServicesAvailable =
    GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)

  private lazy val connectionCallbacks = new ConnectionCallbacks(this)

  lazy val apiClient = builder.build

  def builder = new GoogleApiClient.Builder(context)
    .addApi(api)
    .addConnectionCallbacks(connectionCallbacks)
    .addOnConnectionFailedListener(connectionCallbacks)

  protected def api: Api[_ <: Api.ApiOptions.NotRequiredOptions]

  def apiConnectionFailed(connectionResult: ConnectionResult) {
    send(ConnectionLost)
  }

  def apiConnected(data: Bundle) = {
    send(ConnectionEstablished)
  }

  lazy val isConnected = async.signalOf(false)

  lazy val failed = async.signalOf(false)

  val transitions: ViewTransitions = {
    case Connect ⇒ connect
    case ConnectionEstablished ⇒ connectionEstablished
    case ConnectionLost ⇒ connectionLost
  }

  def connect: ViewTransition = {
    case S(Disconnected, d) ⇒
      S(Connecting, d) <<
        stateEffect("connecting play services") { apiClient.connect() }
  }

  def disconnect: ViewTransition = {
    case S(_, d) ⇒
      S(Disconnected, d) <<
        stateEffect("disconnecting play services") { apiClient.disconnect() }
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
