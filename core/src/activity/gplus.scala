package tryp

import rx._
import rx.ops._

import com.google.android.gms.common._
import com.google.android.gms.auth._

import ViewState._

trait GPlusImpl
extends StateImpl
{
  case object Fetching extends BasicState

  case object Auth extends Message

  def auth: ViewTransition = {
    case s ⇒ s
  }

  def create: ViewTransition = {
    case S(Pristine, data) ⇒
      S(Initialized, data) << autoFetchAuthToken.option(fetchToken)
  }

  val transitions: ViewTransitions = {
    case Create(_, _) ⇒ create
    case Auth ⇒ auth
  }

  def fetchToken = Nop

  def authorizeToken: AppEffect

  def autoFetchAuthToken = true
}

trait GPlusIntegration
extends ActivityBase
with AppPreferences
with StatefulActivity
{ self: Akkativity ⇒

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    authTokenObserver
  }

  val gPlusImpl: GPlusImpl

  override def impls = gPlusImpl :: super.impls

  var fetchingPlusToken = false

  def authToken: Var[String]

  def authorizeToken(account: String, plusToken: String)

  lazy val authTokenState = Rx { (authToken(), GPlus.signedIn()) }

  def autoFetchAuthToken = true

  lazy val authTokenObserver = Obs(authTokenState) {
    if (autoFetchAuthToken && authToken() == "" && !fetchingPlusToken) {
      obtainToken()
    }
  }

  def obtainToken() {
    fetchingPlusToken = true
    fetchPlusToken.future andThen {
      case Success((account, token)) ⇒
        Log.i(s"Successfully obtained plus token: $token")
        authorizeToken(account, token)
        GoogleAuthUtil.clearToken(this, token)
      case Failure(ex: UserRecoverableAuthException) ⇒
        requestPermission(ex.getIntent)
      case Failure(ex) ⇒
        Log.e(s"Google Plus auth token request failed: $ex")
    } onComplete { _ ⇒ fetchingPlusToken = false }
  }

  def serverClientId(implicit c: Context) =
    res.string("gplus_oauth_server_client_id")

  val scopes = Scopes.PLUS_LOGIN

  def scopeString(implicit c: Context) =
    s"oauth2:server:client_id:$serverClientId:api_scope:$scopes"

  def fetchPlusToken = {
    GPlus { plus ⇒
      plus.email map {e ⇒ (e, plusToken(e)) } getOrElse {
        throw new Exception("Plus account name unavailable")
      }
    }
  }

  def plusToken(email: String) =
    GoogleAuthUtil.getToken(this, email, scopeString)

  def requestPermission(intent: Intent) =
    startActivityForResult(intent, GPlusBase.RC_TOKEN_FAIL)

  def plusTokenResolved(success: Boolean) {
    if (success) obtainToken()
  }

  override def onActivityResult(requestCode: Int, responseCode: Int, intent:
    Intent)
  {
    if (requestCode == GPlusBase.RC_SIGN_IN)
      GPlus.signInComplete(responseCode == android.app.Activity.RESULT_OK)
    else if (requestCode == GPlusBase.RC_TOKEN_FAIL)
      plusTokenResolved(responseCode == android.app.Activity.RESULT_OK)
  }
}
