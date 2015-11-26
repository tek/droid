package tryp.droid

import scalaz._, Scalaz._, concurrent._
import scalaz.syntax.std.all._

import rx._
import rx.ops._

import com.google.android.gms.common.Scopes
import com.google.android.gms.auth.UserRecoverableAuthException

import state._

object AuthState
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

abstract class AuthState
(implicit ctx: AuthStateUiI, res: Resources, plus: PlusInterface,
  mt: MessageTopic, settings: Settings)
extends DroidState[AuthStateUiI]
{
  import AuthState._

  override def handle = "gplus"

  case class BackendData(token: String)
  extends Data

  val transitions: ViewTransitions = {
    case Resume ⇒ resume
    case Reset ⇒ reset
    case Fetch ⇒ fetch
    case AuthorizeToken(account, token) ⇒ auth(account, token)
    case BackendAuthorized(token) ⇒ storeToken(token)
    case RequestPermission(intent) ⇒ requestPermission(intent)
    case FetchTokenFailed(error) ⇒ fetchTokenFailed(error)
  }

  def resume: ViewTransition = {
    case S(Pristine, data) ⇒
      if (backendTokenValid) S(Authed, BackendData(backendToken()))
      else S(Unauthed, data) << autoFetchAuthToken().option(Fetch)
  }

  def reset: ViewTransition = {
    case _ ⇒
      S(Unauthed, NoData) <<
        stateEffect("clear backend auth token") { backendToken() = "" }
  }

  def fetch: ViewTransition = {
    case s @ S(Unauthed, data) ⇒
      S(Fetching, data) << fetchToken
  }

  def auth(account: String, plusToken: String): ViewTransition = {
    case S(Fetching, data) ⇒
      S(Authing, data) << authorizePlusToken(account, plusToken) <<
        clearPlusToken(plusToken)
  }

  def storeToken(token: String): ViewTransition = {
    case S(_, data) ⇒
      S(Authed, BackendData(token)) << Toast("backend_auth_success") <<
        stateEffect("store backend auth token") { backendToken() = token }
  }

  def fetchTokenFailed(error: String): ViewTransition = {
    case S(_, _) ⇒
      S(Unauthed, NoData) << LogError("requesting plus token", error).toResult
  }

  def requestPermission(intent: Intent): ViewTransition = {
    case s @ S(Unauthed, data) ⇒
      s << stateEffect("initiating plus permission request") {
        ctx.startActivity(intent, Plus.RC_TOKEN_FETCH)
    }
  }

  // FIXME blocking indefinitely if the connection failed
  def fetchToken: Task[Option[Result]] = {
    val err = FetchTokenFailed("Plus account name unavailable").toFail
    plus.oneAccount
      .map(_.email cata(tokenFromEmail, err))
      .runLast
  }

  def tokenFromEmail(email: String): Result = {
    Try(plusToken(email)) match {
      case Success(tkn) ⇒
        AuthorizeToken(email, tkn).toResult
      case Failure(t: UserRecoverableAuthException) ⇒
        NonEmptyList(
          FetchTokenFailed("insufficient permissions"),
          RequestPermission(t.getIntent)
        ).failure[Message]
      case Failure(t) ⇒
        NonEmptyList(
          LogFatal("requesting plus token", t),
          FetchTokenFailed("exception thrown")
        ).failure[Message]
    }
  }

  def serverClientId = res.string("gplus_oauth_server_client_id")

  val scopes = Scopes.PLUS_LOGIN

  def scope = s"oauth2:server:client_id:$serverClientId:api_scope:$scopes"

  def plusToken(email: String) = ctx.plusToken(email, scope)

  def backend: Backend

  def backendToken = backend.token

  lazy val autoFetchAuthToken =
    settings.app.bool("auto_fetch_auth_token", true)

  def authorizePlusToken(account: String, plusToken: String): Effect = {
    Task {
      backend.authorizePlusToken(account, plusToken) map(BackendAuthorized(_))
    }
  }

  def clearPlusToken(token: String) =
    stateEffect("clear plus token") { ctx.clearPlusToken(token) }

  def plusTokenResolved(success: Boolean) {
    if (success) send(Fetch)
  }

  def backendTokenValid = {
    !backendToken().isEmpty
  }
}

trait AuthIntegration
extends ActivityBase
with AppPreferences
with ActivityAgent
with ResourcesAccess
with HasPlus
{ act: Akkativity ⇒

  lazy val authMachine = new AuthState {
    def backend = new Backend()(settings, res)
  }

  override def machines = authMachine :: super.machines

  override def onActivityResult(requestCode: Int, responseCode: Int,
    intent: Intent) = {
    Log.i(s"activity result: request $requestCode, response $responseCode")
    val ok = responseCode == android.app.Activity.RESULT_OK
    if (requestCode == Plus.RC_SIGN_IN)
      plus.send(PlayServices.Connect)
    else if (requestCode == Plus.RC_TOKEN_FETCH)
      authMachine.plusTokenResolved(ok)
  }

  def obtainToken() = {
    sendAll(NonEmptyList(AuthState.Reset, AuthState.Fetch))
  }
}
