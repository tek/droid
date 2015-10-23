package tryp.droid

import concurrent.ExecutionContext.Implicits.global

import scalaz._, Scalaz._, concurrent._
import scalaz.syntax.std.all._

import rx._
import rx.ops._

import com.google.android.gms.common._
import com.google.android.gms.auth._

import ViewState._

object AuthMessages
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

  case class TokenFetchResult(data: Try[\/[String, (String, String)]])
  extends Message
}
import AuthMessages._

trait AuthImpl
extends StateImpl
with HasActivity
{
  override def description = "gplus state"

  case object Fetching extends BasicState
  case object Authing extends BasicState
  case object Unauthed extends BasicState
  case object Authed extends BasicState

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
    case TokenFetchResult(data) ⇒ tokenFetchResult(data)
  }

  def resume: ViewTransition = {
    case S(Pristine, data) ⇒
      if (backendTokenValid) S(Authed, BackendData(backendToken()))
      else S(Unauthed, data) << autoFetchAuthToken.option(Fetch)
  }

  def reset: ViewTransition = {
    case _ ⇒
      S(Unauthed, NoData)
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
      activity.startActivityForResult(intent, GPlusBase.RC_TOKEN_FETCH)
    }
  }

  def tokenFetchResult
  (result: Try[\/[String, (String, String)]]) : ViewTransition = {
    case s @ S(Fetching, data) ⇒
      result match {
        case Success(-\/(e)) ⇒
          s << FetchTokenFailed(e)
        case Success(\/-((a, t))) ⇒
          s << AuthorizeToken(a, t)
        case Failure(t: UserRecoverableAuthException) ⇒
          s << FetchTokenFailed("insufficient permissions") <<
            RequestPermission(t.getIntent).toResult
        case Failure(t) ⇒
          s << LogFatal("requesting plus token", t).toResult <<
            FetchTokenFailed("exception thrown")
      }
  }

  // TODO change to task
  def fetchToken: AppEffect = {
    stateEffect("initiating plus token acquisition") {
      GPlus {
        _.email
          .map(e ⇒ e → plusToken(e))
          .toRightDisjunction("Plus account name unavailable")
      }
      .future
      .onComplete(TokenFetchResult.apply _ >>> send _)
    }
  }

  def serverClientId(implicit c: Context) =
    res.string("gplus_oauth_server_client_id")

  val scopes = Scopes.PLUS_LOGIN

  def scopeString(implicit c: Context) =
    s"oauth2:server:client_id:$serverClientId:api_scope:$scopes"

  def plusToken(email: String) =
    GoogleAuthUtil.getToken(activity, email, scopeString)

  def backendToken = backend.token

  def autoFetchAuthToken = true

  def authorizePlusToken(account: String, plusToken: String): AppEffect = {
    Task {
      backend.authorizePlusToken(account, plusToken) map(BackendAuthorized(_))
    }
  }

  def clearPlusToken(token: String): AppEffect =
    stateEffect("clear plus token") {
      GoogleAuthUtil.clearToken(context, token)
    }

  val backend = new Backend

  def plusTokenResolved(success: Boolean) {
    if (success) send(Fetch)
  }

  def backendTokenValid = !backendToken().isEmpty
}

trait AuthIntegration
extends ActivityBase
with AppPreferences
with StatefulActivity
{ act: Akkativity ⇒

  val gPlusImpl = new AuthImpl {
    def activity = act
  }

  override def impls = gPlusImpl :: super.impls

  override def onActivityResult(requestCode: Int, responseCode: Int,
    intent: Intent) = {
    val ok = responseCode == android.app.Activity.RESULT_OK
    if (requestCode == GPlusBase.RC_SIGN_IN)
      GPlus.signInComplete(ok)
    else if (requestCode == GPlusBase.RC_TOKEN_FETCH)
      gPlusImpl.plusTokenResolved(ok)
  }

  def obtainToken() = {
    sendAll(Reset <:: Fetch.wrapNel)
  }
}
