package tryp
package droid
package test

import state.{PlayServices, PlusAccount}
import state.service.PlusInterface
import PlayServices._

import com.google.android.gms
import gms.common.{api => gapi}
import gapi.GoogleApiClient

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
  override def connect: Transit = {
    case s => s << ConnectionEstablished
  }

  override def apiConnected(data: Bundle) {
  }

  override def account = client map(_.map(new MockPlusAccount(_)))
}

// trait AuthStateMock
// extends FreeActivityAgent
// with AuthIntegration
// {

//   override implicit lazy val plus: PlusInterface = new MockPlusInterface {}

//   override lazy val authMachine = new AuthState {
//     def activity = act

//     def backend = new Backend()(settings, res)

//     override def plusToken(email: String) = "mock_plus_token"

//     override def clearPlusToken(token: String) = Nop

//     override def authorizePlusToken(account: String, plusToken: String) = {
//       AuthStateData.BackendAuthorized(MockData.authToken)
//     }
//   }
// }
