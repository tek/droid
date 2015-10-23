package tryp.droid
package test

import scalaz._, Scalaz._, concurrent._

object MockData
{
  val email = "test@gmail.com"
  val authToken = "authtoken"
}

class MockGPlus(implicit val context: Context)
extends GPlus
{
  override def connect() { }

  def apiConnected(data: Bundle) {
  }
}

class GPlusMock
extends GPlusBase
{
  class MockAccount(implicit c: Context)
  extends Account(new MockGPlus)
  {
    override def email = Some(MockData.email)
  }

  signedIn() = true

  override def apply[A](callback: PlusCallback[A])(implicit a: Activity) = {
    val promise = Promise[\/[String, A]]()
    Task(callback(new MockAccount)) runAsync {
      case \/-(result) ⇒ promise success result
      case -\/(error) ⇒ promise failure error
    }
    promise
  }
}

trait AuthStateMock
extends StatefulActivity
with AuthIntegration
{ act: TrypActivity ⇒

  override val gPlusImpl = new AuthImpl {
    def activity = act

    override def plusToken(email: String) = "mock_plus_token"

    override def clearPlusToken(token: String) = ViewState.Nop

    override def authorizePlusToken(account: String, plusToken: String) = {
      AuthMessages.BackendAuthorized(MockData.authToken).success
    }
  }
}
