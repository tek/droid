package tryp
package droid
package test

import scalaz._, Scalaz._, concurrent._

import com.google.android.gms
import gms.common.{api ⇒ gapi}
import gapi.GoogleApiClient

import droid.State._
import PlayServices._

object MockData
{
  val email = "test@gmail.com"
  val authToken = "authtoken"
}

class MockPlusAccount(apiClient: GoogleApiClient)
extends PlusAccount(apiClient)
{
  override def email = MockData.email.just
}

trait MockPlusInterface
extends PlusInterface
{
  override def connect: ViewTransition = {
    case s ⇒ s << ConnectionEstablished
  }


  override def apiConnected(data: Bundle) {
  }

  override def account = client map(new MockPlusAccount(_))
}

trait AuthStateMock
extends ActivityAgent
with AuthIntegration
{ act: TrypActivity ⇒

  override implicit lazy val plus: PlusInterface = new MockPlusInterface {}

  override lazy val authImpl = new AuthState {
    def activity = act

    def backend = new Backend()(settings, res)

    override def plusToken(email: String) = "mock_plus_token"

    override def clearPlusToken(token: String) = droid.State.Nop

    override def authorizePlusToken(account: String, plusToken: String) = {
      AuthState.BackendAuthorized(MockData.authToken).success
    }
  }
}
