package tryp.droid

import android.os.Bundle

import com.google.android.gms.location.{LocationServices,_}
import LocationServices.{FusedLocationApi ⇒ Api}
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient

trait Locations
extends tryp.droid.Basic
with tryp.droid.util.CallbackMixin
{
  class LocationCallbacks(owner: Locations)
  extends GoogleApiClient.OnConnectionFailedListener
  with GoogleApiClient.ConnectionCallbacks
  {
    def onConnected(data: Bundle) {
      owner.locationConnected(data)
    }

    def onConnectionFailed(connectionResult: ConnectionResult) {
    }

    def onConnectionSuspended(cause: Int) {
    }
  }

  private lazy val locationCallbacks = new LocationCallbacks(this)

  private lazy val apiClient =
    new GoogleApiClient.Builder(context)
    .addApi(LocationServices.API)
    .addConnectionCallbacks(locationCallbacks)
    .addOnConnectionFailedListener(locationCallbacks)
    .build


  abstract override def onStart {
    super.onStart
    apiClient.connect
  }

  abstract override def onStop {
    super.onStop
    apiClient.disconnect
  }

  def locations = apiClient.isConnected ? apiClient

  def lastLocation = {
    locations map { Api.getLastLocation(_) }
    }

  def locationConnected(data: Bundle)
}
