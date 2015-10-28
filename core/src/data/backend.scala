package tryp
package droid

import java.util.concurrent.TimeUnit

import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._

import com.squareup.okhttp.{Request ⇒ OkRequest, _}

import scalaz._, Scalaz._

import argonaut._, Argonaut._

import akka.pattern.ask

import _root_.slick.dbio.DBIO

import res.ResourcesAccess
import slick.sync._
import SyncResult._

object Backend
{
  lazy val client = (new OkHttpClient) tap { c ⇒
      c.setConnectTimeout(10, TimeUnit.SECONDS)
      c.setWriteTimeout(10, TimeUnit.SECONDS)
      c.setReadTimeout(30, TimeUnit.SECONDS)
  }
}

class Backend(implicit c: Context)
extends ResourcesAccess
{
  def token = res.appPrefs.string("backend_token")

  def host = res.string("backend_host")

  def port = res.integer("backend_port")

  def scheme = res.string("backend_scheme")

  val mediaTypeJson = MediaType.parse("application/json")

  def request(req: Request)
  (method: OkRequest.Builder ⇒ OkRequest.Builder) = {
    authenticated { () ⇒ uncheckedRequest(req)(method) }
  }

  def urlBuilder = (new HttpUrl.Builder)
      .scheme(scheme)
      .host(host)
      .port(port)

  def uncheckedRequest(req: Request)
  (method: OkRequest.Builder ⇒ OkRequest.Builder) = {
    val token = this.token
    val basic = urlBuilder.addPathSegment("user")
    val withParams = req.params.foldLeft(basic) {
      case (builder, (k, v)) ⇒ builder addQueryParameter(k, v)
    }
    val withPath = req.segments.foldLeft(withParams) {
      (builder, seg) ⇒ builder addPathSegment(seg)
    }
    val builder = (new OkRequest.Builder)
      .url(withPath.build)
      .header("access-token", token())
    call(method(builder).build)
  }

  def call(request: OkRequest) = {
    val response = Backend.client.newCall(request).execute()
    if (response.isSuccessful) response.body.string.right
    else {
      val message = Option(response.message) getOrElse "No message"
      s"Http request failed with status ${response.code}: ${message}".left
    }
  }

  def authorizePlusToken(account: String, plusToken: String) = {
    val json = ("id" := account) ->: ("token" := plusToken) ->: jEmptyObject
    val body = RequestBody.create(mediaTypeJson, json.spaces2)
    val request = (new OkRequest.Builder)
      .url(urlBuilder.addPathSegment("token").build)
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
    uncheckedRequest(Request.at("ping"))(identity) match {
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
with RestClient
{
  def jsonRequest(req: Request)
  (method: OkRequest.Builder ⇒ RequestBody ⇒ OkRequest.Builder) = {
    val body = RequestBody.create(mediaTypeJson, req.body getOrElse("{}"))
    request(req)(r ⇒ method(r)(body))
  }

  def post(req: Request) =
    jsonRequest(req) { _.post _ }

  def put(req: Request) =
    jsonRequest(req) { _.put _ }

  def get(req: Request) =
    request(req)(identity)

  def delete(req: Request) =
    jsonRequest(req) { _.delete _ }
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
          messages.toList match {
            case head :: tail ⇒
              tryBackend(next andThen(callback))(head, tail)
            case Nil ⇒
              next andThen("fatal".syncFail(errmsg, false).wrapDbioSeq)
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
