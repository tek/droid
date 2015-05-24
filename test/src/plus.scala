package tryp.droid.test

import tryp.droid._

object MockData
{
  val email = "test@gmail.com"
  val authToken = "authtoken"
}

class MockGPlus(implicit val context: Context)
extends GPlus
{
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
    import concurrent.ExecutionContext.Implicits.global
    val promise = Promise[A]()
    Future {
      Try(callback(new MockAccount)) match {
        case Success(result) ⇒ promise success result
        case Failure(error) ⇒ promise failure error
      }
    }
    promise
  }
}
