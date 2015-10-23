package tryp.droid

import com.google.android.gms.common._
import com.google.android.gms.common.api._

trait PlayServices
extends tryp.droid.Basic
{
  class ConnectionCallbacks(owner: PlayServices)
  extends GoogleApiClient.OnConnectionFailedListener
  with GoogleApiClient.ConnectionCallbacks
  {
    def onConnected(data: Bundle) {
      owner.apiConnected(data)
    }

    def onConnectionFailed(connectionResult: ConnectionResult) {
      Log.e("Google Api connection failed")
      owner.apiConnectionFailed(connectionResult)
    }

    def onConnectionSuspended(cause: Int) {
      Log.e("Google Api connection suspended")
    }
  }

  def playServicesAvailable =
    GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)

  private lazy val connectionCallbacks = new ConnectionCallbacks(this)

  lazy val apiClient = builder.build

  def builder = new GoogleApiClient.Builder(context)
    .addApi(api)
    .addConnectionCallbacks(connectionCallbacks)
    .addOnConnectionFailedListener(connectionCallbacks)

  protected def api: Api[_ <: Api.ApiOptions.NotRequiredOptions]

  def connect() {
    apiClient.connect()
  }

  def disconnect() {
    apiClient.disconnect()
  }

  def apiConnected(data: Bundle)

  def apiConnectionFailed(connectionResult: ConnectionResult) {
  }
}
