package tryp
package droid
package state

import scalaz.concurrent._, scalaz.stream._

import cats.syntax.all._

import com.google.android.gms
import gms.common.ConnectionResult
import gms.common.{api => gapi}
import gms.plus
import gapi.GoogleApiClient

import state.core._
import state._
import view.core._
import view._

import IOOperation._

object PlusInterface
{
  case object SignOut
  extends Message
}
import PlusInterface._
import PlayServices._

class PlusInterface
extends PlayServices
{
  def subHandle = "plus"

  def api = plus.Plus.API

  override def admit = plusAdmit orElse basicAdmit

  val plusAdmit: Admission = {
    case ConnectionFailed(result) => failed(result)
    case SignOut => signOut
  }

  def failed(result: ConnectionResult): Transit = {
    case s =>
      s << act(_.resolveResult(result, Plus.RC_SIGN_IN))
  }

  def signOut: Transit = {
    case s =>
      // s << apiClient.map(plus.Plus.AccountApi.clearDefaultAccount) <<
      s << apiClient.map(_ => ()) <<
        Disconnect
  }

  def account = client map(_.map(new PlusAccount(_)))

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

// trait HasPlus
// extends TrypActivity
// {
//   implicit lazy val plus = new PlusInterface {}
// }

// trait PlusInterfaceAccess
// extends ActivityAccess
// with HasActivityAgent
// {
//   implicit lazy val plus =
//     activitySub[HasPlus] some(_.plus) none(new PlusInterface {})
// }
