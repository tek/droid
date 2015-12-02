package tryp.droid

import scalaz.{Plus ⇒ _, _}, Scalaz._, concurrent._, stream._

import shapeless.tag.@@

import com.google.android.gms
import gms.common.ConnectionResult
import gms.common.{api ⇒ gapi}
import gms.plus
import gapi.GoogleApiClient

import state._

object PlusInterface
{
  case object SignOut
  extends Message
}
import PlusInterface._
import PlayServices._

class PlusInterface(implicit val ctx: StartActivity, mt: MessageTopic @@ To)
extends PlayServices[StartActivity]
{
  def subHandle = "plus"

  def api = plus.Plus.API

  override def admit = plusAdmit orElse basicAdmit

  val plusAdmit: Admission = {
    case ConnectionFailed(result) ⇒ failed(result)
    case SignOut ⇒ signOut
  }

  def failed(result: ConnectionResult): Transit = {
    case s ⇒
      s << stateEffect("start plus sign-in activity") {
        ctx.resolveResult(result, Plus.RC_SIGN_IN)
      }
  }

  def signOut: Transit = {
    case s ⇒
      s << stateEffect("sign out of gplus") {
        apiClient foreach(plus.Plus.AccountApi.clearDefaultAccount)
    } << Disconnect
  }

  def account = client map(new PlusAccount(_))

  def oneAccount = {
    send(Connect)
    account |> Process.await1
  }

  override def builder =
    super.builder map(_.addScope(plus.Plus.SCOPE_PLUS_LOGIN))

}

object Plus
{
  val RC_SIGN_IN = 1
  val RC_TOKEN_FETCH = 2
}

trait HasPlus
extends TrypActivity
{
  implicit lazy val plus = new PlusInterface {}
}

trait PlusInterfaceAccess
extends ActivityAccess
with HasActivityAgent
{
  implicit lazy val plus =
    activitySub[HasPlus] some(_.plus) none(new PlusInterface {})
}
