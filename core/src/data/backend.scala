package tryp

import java.util.concurrent.TimeUnit

import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._

import com.squareup.okhttp._

import scalaz._, Scalaz._

import argonaut._, Argonaut._

import akka.pattern.ask

import _root_.slick.dbio.DBIO

import res.ResourcesAccess
import slick.sync._
import SyncResult._

class Backend(implicit c: Context)
extends ResourcesAccess
{
  def token = res.appPrefs.string("backend_token")

  def baseUrl = res.string("backend_base_url")

  def client = (new OkHttpClient) tap { c ⇒
      c.setConnectTimeout(10, TimeUnit.SECONDS)
      c.setWriteTimeout(10, TimeUnit.SECONDS)
      c.setReadTimeout(30, TimeUnit.SECONDS)
  }

  val mediaTypeJson = MediaType.parse("application/json")

  def request(path: String)
  (method: Request.Builder ⇒ Request.Builder) = {
    authenticated { () ⇒ uncheckedRequest(path)(method) }
  }

  def uncheckedRequest(path: String)
  (method: Request.Builder ⇒ Request.Builder) = {
    val token = this.token
    val builder = (new Request.Builder)
      .url(s"$baseUrl/user$path")
      .header("access-token", token())
    call(method(builder).build)
  }

  def call(request: Request) = {
    val response = client.newCall(request).execute()
    if (response.isSuccessful) response.body.string.right
    else {
      val message = Option(response.message) getOrElse "No message"
      s"Http request failed with status ${response.code}: ${message}".left
    }
  }

  def authorizePlusToken(account: String, plusToken: String) = {
    val json = ("id" := account) ->: ("token" := plusToken) ->: jEmptyObject
    val body = RequestBody.create(mediaTypeJson, json.spaces2)
    val request = (new Request.Builder)
      .url(s"${baseUrl}/token")
      .post(body)
      .build
    call(request) match {
      case \/-(result) ⇒
        Log.i(s"Backend authentication successful: $result")
        token.update(result)
        result.successNel[String]
      case -\/(err) ⇒
        Log.e(s"Backend authentication failed: $err")
        err.toString.failureNel[String]
    }
  }

  def ping() = {
    uncheckedRequest("/ping")(identity) match {
      case \/-(_) ⇒ true
      case -\/(error) ⇒
        Log.e(s"Not authenticated at backend: $error (token: ${token()})")
        false
    }
  }

  def authenticated[A](callback: () ⇒ A) = {
    if (!ping()) throw AuthError("backend auth failed")
    else callback()
  }
}

class BackendRestClient(implicit c: Context)
extends Backend
with tryp.slick.sync.RestClient
{
  def jsonRequest(path: String, json: String)
  (method: Request.Builder ⇒ RequestBody ⇒ Request.Builder) = {
    val body = RequestBody.create(mediaTypeJson, json)
    request(path)(r ⇒ method(r)(body))
  }

  def post(path: String, json: String = "{}") =
    jsonRequest(path, json) { _.post _ }

  def put(path: String, json: String = "{}") =
    jsonRequest(path, json) { _.put _ }

  def get(path: String, json: String = "{}") =
    request(path)(identity)

  def delete(path: String, json: String = "{}") =
    jsonRequest(path, json) { _.delete _ }
}

trait BackendAccess
extends DbAccess
{
  self: HasContext ⇒

  def tryBackend(callback: ResultsAction)
  (message: Any, messages: Any*): ResultsAction = {
    callback
      .recoverWith {
        case AuthError(error) ⇒
          val errmsg = s"not authed with backend: $error"
          implicit val timeout = akka.util.Timeout(10 seconds)
          val next = DBIO.from(self.core ? message)
          messages match {
            case head :: tail ⇒
              tryBackend(next andThen(callback))(head, tail)
            case Nil ⇒ next andThen(errmsg.syncFail)
          }
      }
  }

  def backendAuthenticated(callback: ResultsAction) = {
    tryBackend(callback)(
      Messages.AuthBackend(), Messages.Toast("backend_auth_failed"))
  }
}

abstract class DroidBackendSync(syncExclude: List[String])
extends BackendAccess
with HasContext
with DbAccess
{
  def process(schema: SyncSchema)
  (implicit ec: EC, rest: RestClient): ResultsAction = {
    backendAuthenticated { BackendSync(syncExclude)(schema) }
  }
}
