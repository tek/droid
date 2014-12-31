package tryp.droid

import android.app.PendingIntent

import com.google.android.gms.location.{LocationServices,_}
import LocationServices.{FusedLocationApi ⇒ LocApi, GeofencingApi ⇒ GeoApi}
import com.google.android.gms.common._
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.common.api._

case class GeofenceData(id: String, lat: Double, long: Double)

trait Locations
extends tryp.droid.Basic
with tryp.droid.util.CallbackMixin
with LocationListener
{
  class LocationCallbacks(owner: Locations)
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
    GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)

  private lazy val locationCallbacks = new LocationCallbacks(this)

  lazy val apiClient =
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
    locations flatMap { client ⇒ Option(LocApi.getLastLocation(client)) }
    }

  def requestGeofences(
    intent: PendingIntent, locations: Seq[GeofenceData]
  ) {
    val builder = new GeofencingRequest.Builder
    locations foreach { loc ⇒ builder.addGeofence(geofence(loc)) }
    GeoApi.addGeofences(apiClient, builder.build, intent)
  }

  val expirationDuration = 24 * 3600 * 1000

  def geofence(fence: GeofenceData) = {
    new Geofence.Builder()
      .setRequestId(fence.id.toString)
      .setTransitionTypes(
        Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL
        | Geofence.GEOFENCE_TRANSITION_EXIT
      )
      .setCircularRegion(fence.lat, fence.long, 50)
      .setExpirationDuration(expirationDuration)
      .setLoiteringDelay(500)
      .build
  }

  def requestLocationUpdates {
    val request = LocationRequest.create
      .setInterval(5)
      .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    LocApi.requestLocationUpdates(apiClient, request, this)
  }

  def locationConnected(data: Bundle)
}
