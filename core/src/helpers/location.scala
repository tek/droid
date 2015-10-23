package tryp.droid

import android.app.PendingIntent
import android.location.Location

import com.google.android.gms.location.{LocationServices,_}
import LocationServices.{FusedLocationApi ⇒ LocApi, GeofencingApi ⇒ GeoApi}
import com.google.android.gms.common._
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.common.api._

case class GeofenceData(id: String, lat: Double, long: Double)

trait LocationsConcern
extends tryp.droid.Basic
with LocationListener
with tryp.droid.Preferences
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
    GooglePlayServicesUtil.isGooglePlayServicesAvailable(context)

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
    locations flatMap { client ⇒ Option(LocApi.getLastLocation(client)) }
    }

  def requestGeofences(intent: PendingIntent, locations: Seq[GeofenceData])
  (callback: (Int) ⇒ Unit) {
    val builder = new GeofencingRequest.Builder
    locations foreach { loc ⇒ builder.addGeofence(geofence(loc)) }
    Try {
      GeoApi.addGeofences(apiClient, builder.build, intent)
    } match {
      case Failure(e) ⇒
        callback(-1)
        Log.e(s"Adding geofences failed (${e})")
      case Success(result) ⇒
        result.setResultCallback(new ResultCallback[Status] {
          def onResult(status: Status) {
            reportGeofenceResult(status)
            callback(status.getStatusCode)
          }
        })
    }
  }

  def reportGeofenceResult(status: Status) {
    import GeofenceStatusCodes._
    status.getStatusCode match {
      case GEOFENCE_NOT_AVAILABLE ⇒ Log.e("Geofence service down")
      case GEOFENCE_TOO_MANY_GEOFENCES ⇒ Log.e("Too many geofences")
      case GEOFENCE_TOO_MANY_PENDING_INTENTS ⇒
        Log.e("Too many pending geofence intents")
      case _ ⇒ Log.i("Geofences installed successfully")
    }
  }

  val expirationDuration = 24 * 3600 * 1000

  def alarmDistance = prefs.int("alarm_distance", 100)

  def geofence(fence: GeofenceData) = {
    new Geofence.Builder()
      .setRequestId(fence.id.toString)
      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
      .setCircularRegion(fence.lat, fence.long, alarmDistance())
      .setExpirationDuration(expirationDuration)
      .build
  }

  def requestLocationUpdates(intent: PendingIntent) {
    val request = LocationRequest.create
      .setInterval(5 * 60 * 1000)
      .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    LocApi.requestLocationUpdates(apiClient, request, intent)
  }

  def locationConnected(data: Bundle)
}

trait Locations
extends LocationsConcern
with CallbackMixin
{
  abstract override def onStart() {
    super.onStart()
    connect()
  }

  abstract override def onStop() {
    super.onStop()
    disconnect()
  }
}

case class LocationTask(callback: (Location) ⇒ Unit)
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
      .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    LocApi.requestLocationUpdates(apiClient, request, this)
  }

  def onLocationChanged(location: Location) {
    callback(location)
    LocApi.removeLocationUpdates(apiClient, this)
    disconnect()
    done = true
  }
}

case class GeofenceTask(intent: PendingIntent, fences: Seq[GeofenceData])
(callback: (Int) ⇒ Unit)(implicit val context: Context)
extends LocationsConcern
{
  def run() {
    connect()
  }

  def locationConnected(data: Bundle) {
    requestGeofences(intent, fences) { s ⇒
      disconnect()
      callback(s)
    }
  }

  def onLocationChanged(location: Location) {
  }
}
