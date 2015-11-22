package tryp
package droid

import scalaz._, Scalaz._, concurrent._, stream._, Process._

import android.app.PendingIntent

import com.google.android.gms.common._
import com.google.android.gms.location._
import LocationServices.GeofencingApi
import com.google.android.gms.common.api._

import State._

case class GeofenceInterface(apiClient: GoogleApiClient, intent: PendingIntent)
(implicit prefs: Settings)
{
  def request(locations: Seq[GeofenceData]) = {
    val builder = new GeofencingRequest.Builder
    locations foreach { loc ⇒ builder.addGeofence(geofence(loc)) }
    Task(GeofencingApi.addGeofences(apiClient, builder.build, intent))
      .map(attachResultCallback("installed", locations))
  }

  def clear = {
    Task(GeofencingApi.removeGeofences(apiClient, intent))
      .map(attachResultCallback("removed", Nil))
  }

  def attachResultCallback(action: String, locations: Seq[GeofenceData])
  (result: PendingResult[Status]) = {
    result.setResultCallback(new ResultCallback[Status] {
      def onResult(status: Status) {
        reportGeofenceResult(status, action)
        storeGeofenceIds(locations)
      }
    })
  }

  def storeGeofenceIds(locations: Seq[GeofenceData]) = {
    prefs.app.set("active_geofences", locations map(_.id))
  }

  def reportGeofenceResult(status: Status, action: String) {
    import GeofenceStatusCodes._
    status.getStatusCode match {
      case GEOFENCE_NOT_AVAILABLE ⇒ Log.e("Geofence service down")
      case GEOFENCE_TOO_MANY_GEOFENCES ⇒ Log.e("Too many geofences (>100)")
      case GEOFENCE_TOO_MANY_PENDING_INTENTS ⇒
        Log.e("Too many pending geofence intents (>5)")
      case _ ⇒ Log.i(s"Geofences $action successfully")
    }
  }

  val expirationDuration = 24 * 3600 * 1000

  def alarmDistance = prefs.user.int("alarm_distance", 100)

  def geofence(fence: GeofenceData) = {
    new Geofence.Builder()
      .setRequestId(fence.id.toString)
      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
      .setCircularRegion(fence.lat, fence.long, alarmDistance())
      .setExpirationDuration(expirationDuration)
      .build
  }
}

case class GeofenceHandler(intent: PendingIntent)
(implicit val context: Context)
extends Stateful
{
  // TODO connect
  def success = async.signalOf(false)

  def installed: Boolean = success.get.attemptRun | false

  lazy val location = new LocationInterface {}

  override def impls = location :: super.impls
  
  def setupProcess(client: GoogleApiClient, locations: Seq[GeofenceData]) = {
    val geofences = GeofenceInterface(client, intent)
    Process.eval_(geofences.clear) ++
      Process.eval_(geofences.request(locations))
  }

  def setup(locations: Seq[GeofenceData]) = {
    location.oneClient flatMap(c ⇒ setupProcess(c, locations))
  }
}