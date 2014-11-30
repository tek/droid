package tryp.droid

import android.os.Bundle

import com.google.android.gms.location._
import com.google.android.gms.common.GooglePlayServicesClient
import com.google.android.gms.common.ConnectionResult

trait Locations
extends tryp.droid.Basic
with tryp.droid.util.CallbackMixin
{
  class LocationCallbacks(owner: Locations)
  extends GooglePlayServicesClient.OnConnectionFailedListener
  with GooglePlayServicesClient.ConnectionCallbacks
  {
    def onConnected(data: Bundle) {
      owner.locationConnected(data)
    }

    def onDisconnected {
    }

    def onConnectionFailed(connectionResult: ConnectionResult) {
    }
  }

  private lazy val locationCallbacks = new LocationCallbacks(this)

  private lazy val locationClient = {
    new LocationClient(context, locationCallbacks, locationCallbacks)
  }

  abstract override def onStart {
    super.onStart
    locationClient.connect
  }

  abstract override def onStop {
    super.onStop
    locationClient.disconnect
  }

  def withLocations[A](callback: (LocationClient) ⇒ A) = {
    locations map { callback(_) }
  }

  def locations = {
    locationClient.isConnected ? locationClient
  }

  def locationConnected(data: Bundle)
}
