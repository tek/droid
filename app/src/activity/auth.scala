package tryp
package droid

import rx._
import rx.ops._

import com.google.android.gms.common.Scopes
import com.google.android.gms.auth.UserRecoverableAuthException

import view._
import view.core._
import state._
import state.core._
import ZS._
import IOOperation._

object AuthStateData
{
  case object Reset extends Message
  case object Fetch extends Message

  case class AuthorizeToken(account: String, token: String)
  extends Message

  case class BackendAuthorized(token: String)
  extends Message

  case class RequestPermission(intent: Intent)
  extends Message

  case class FetchTokenFailed(error: String)
  extends Message

  case object Fetching extends BasicState
  case object Authing extends BasicState
  case object Unauthed extends BasicState
  case object Authed extends BasicState
}

import AuthStateData._

@Publish(AuthorizeToken)
abstract class AuthState
extends Machine
{
  override def handle = "gplus"

  case class BackendData(token: String)
  extends Data

  val admit: Admission = {
    case Resume => resume
    case Reset => reset
    case Fetch => fetch
    case AuthorizeToken(account, token) => auth(account, token)
    case BackendAuthorized(token) => storeToken(token)
    case RequestPermission(intent) => requestPermission(intent)
    case FetchTokenFailed(error) => fetchTokenFailed(error)
  }

  def resume: Transit = {
    case S(Pristine, data) =>
      if (backendTokenValid) S(Authed, BackendData(backendToken()))
      else S(Unauthed, data) << autoFetchAuthToken().opt(Fetch)
  }

  def reset: Transit = {
    case _ =>
      S(Unauthed, NoData) <<
        stateEffect("clear backend auth token") { backendToken() = "" }
  }

  def fetch: Transit = {
    case s @ S(Unauthed, data) =>
      S(Fetching, data)
      // << fetchToken
  }

  def auth(account: String, plusToken: String): Transit = {
    case S(Fetching, data) =>
      S(Authing, data) << authorizePlusToken(account, plusToken) <<
        clearPlusToken(plusToken)
  }

  def storeToken(token: String): Transit = {
    case S(_, data) =>
      S(Authed, BackendData(token)) << Toast("backend_auth_success") <<
        stateEffect("store backend auth token") { backendToken() = token }
  }

  def fetchTokenFailed(error: String): Transit = {
    case S(_, _) =>
      S(Unauthed, NoData) << LogError("requesting plus token", error).toResult
  }

  def requestPermission(intent: Intent): Transit = {
    case s @ S(Unauthed, data) =>
      s << act(_.startActivityForResult(intent, Plus.RC_TOKEN_FETCH))
  }

  // // FIXME blocking indefinitely if the connection failed
  // def fetchToken = {
  //   val err = FetchTokenFailed("Plus account name unavailable").toResult
  //   plus.oneAccount
  //     .map(_.map(_.email cata(tokenFromEmail, err)))
  // }

  def tokenFromEmail(email: String): Result = {
    Try(plusToken(email)) match {
      case Success(tkn) =>
        IOTask(tkn.map(AuthorizeToken(email, _).toResult)).internal.success
      case Failure(t: UserRecoverableAuthException) =>
        Nel(
          FetchTokenFailed("insufficient permissions").toParcel,
          RequestPermission(t.getIntent).toParcel
        ).invalid[Parcel]
      case Failure(t) =>
        Nel(
          LogFatal("requesting plus token", t).toParcel,
          FetchTokenFailed("exception thrown").toParcel
        ).invalid[Parcel]
    }
  }

  def serverClientId = res(_.string("gplus_oauth_server_client_id"))

  val scopes = Scopes.PLUS_LOGIN

  def scope = s"oauth2:server:client_id:$serverClientId:api_scope:$scopes"

  def plusToken(email: String) = con(_.plusToken(email, scope))

  def backend: Backend

  def backendToken = backend.token

  lazy val autoFetchAuthToken =
    () => true
  //   settings.app.bool("auto_fetch_auth_token", true)

  def authorizePlusToken(account: String, plusToken: String) = {
    Task {
      backend.authorizePlusToken(account, plusToken) map(BackendAuthorized(_))
    }
  }

  def clearPlusToken(token: String) =
    con(_.clearPlusToken(token))

  def plusTokenResolved(success: Boolean) {
    if (success) send(Fetch)
  }

  def backendTokenValid = {
    !backendToken().isEmpty
  }
}

trait AuthIntegration
extends Agent
{

  lazy val authMachine = new AuthState {
    def backend = null
    // def backend = new Backend()(settings, res)
  }

  override def machines = authMachine %:: super.machines

  // TODO in activity agent
  // publishLocalOne(ActivityResult(requestCode))

  // override def onActivityResult(requestCode: Int, responseCode: Int,
  //   intent: Intent) = {
  //   Log.i(s"activity result: request $requestCode, response $responseCode")
  //   val ok = responseCode == android.app.Activity.RESULT_OK
  //   if (requestCode == Plus.RC_SIGN_IN)
  //     plus.send(PlayServices.Connect)
  //   else if (requestCode == Plus.RC_TOKEN_FETCH)
  //     authMachine.plusTokenResolved(ok)
  // }

  def obtainToken() = {
    publishLocalAll(Nes(Reset, Fetch))
  }
}
