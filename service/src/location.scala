package tryp
package droid
package state

import android.app.PendingIntent
import android.location.Location

import com.google.android.gms.location._
import LocationServices.{FusedLocationApi => LocApi}
import com.google.android.gms.common._
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.common.api._

case class GeofenceData(id: String, lat: Double, long: Double)

abstract class LocationsConcern(implicit context: Context)
extends LocationListener
{
  class LocationCallbacks(owner: LocationsConcern)
  extends GoogleApiClient.OnConnectionFailedListener
  with GoogleApiClient.ConnectionCallbacks
  {
    def onConnected(data: Bundle) {
      owner.locationConnected(data)
    }

    def onConnectionFailed(connectionResult: ConnectionResult) {
      Log.e("Location connection failed")
    }

    def onConnectionSuspended(cause: Int) {
      Log.e("Location connection suspended")
    }
  }

  def playServicesAvailable =
    false
    // GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)

  private lazy val locationCallbacks = new LocationCallbacks(this)

  lazy val apiClient =
    new GoogleApiClient.Builder(context)
    .addApi(LocationServices.API)
    .addConnectionCallbacks(locationCallbacks)
    .addOnConnectionFailedListener(locationCallbacks)
    .build

  def connect() {
    apiClient.connect()
  }

  def disconnect() {
    apiClient.disconnect()
  }

  def locations = apiClient.isConnected option apiClient

  def lastLocation = {
    locations flatMap { client => Option(LocApi.getLastLocation(client)) }
    }

  def requestLocationUpdates(intent: PendingIntent) {
    val request = LocationRequest.create
      .setInterval(5 * 60 * 1000)
      .setPriority(LocationRequest.PRAIORITY_HIGH_ACCURACY)
    LocApi.requestLocationUpdates(apiClient, request, intent)
  }

  def locationConnected(data: Bundle)
}

// trait Locations
// extends LocationsConcern
// with CallbackMixin
// {
//   abstract override def onStart() {
//     super.onStart()
//     connect()
//   }

//   abstract override def onStop() {
//     super.onStop()
//     disconnect()
//   }
// }

case class LocationTask(callback: (Location) => Unit)
(implicit val context: Context)
extends LocationsConcern
{
  var done = true

  def run {
    if (done) {
      done = false
      connect()
    }
  }

  def locationConnected(data: Bundle) {
    val request = LocationRequest.create
      .setNumUpdates(1)
      .setExpirationDuration(10000)
      .setPriority(LocationRequest.PRAIORITY_HIGH_ACCURACY)
    LocApi.requestLocationUpdates(apiClient, request, this)
  }

  def onLocationChanged(location: Location) {
    callback(location)
    LocApi.removeLocationUpdates(apiClient, this)
    disconnect()
    done = true
  }
}

class LocationInterface
extends PlayServices
{
  def subHandle = "location"

  def api = LocationServices.API
}
