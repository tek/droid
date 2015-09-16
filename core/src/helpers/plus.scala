package tryp.droid

import concurrent.ExecutionContext.Implicits.global

import java.net.URL

import android.graphics.drawable.Drawable

import com.google.android.gms.common._
import com.google.android.gms.plus._
import com.google.android.gms.auth._

trait GPlus
extends PlayServices
{
  def api = Plus.API

  override def builder = super.builder.addScope(Plus.SCOPE_PLUS_LOGIN)

  def person = Option(Plus.PeopleApi.getCurrentPerson(apiClient))

  def photo = person flatMap { p ⇒ Option(p.getImage) }

  def cover = person flatMap { p ⇒ Option(p.getCover) } flatMap
    { c ⇒ Option(c.getCoverPhoto) }

  def email = Option(Plus.AccountApi.getAccountName(apiClient))

  def name = person map(_.getName) map
    { n ⇒ s"${n.getGivenName} ${n.getFamilyName}" }
}

case class GPlusTask(callback: GPlusTask ⇒ Unit)
(implicit val context: Context)
extends GPlus
{
  def apiConnected(data: Bundle) {
    callback(this)
  }
}

class GPlusSignOut(implicit val context: Context)
extends GPlus
{
  def apiConnected(data: Bundle) {
    Plus.AccountApi.clearDefaultAccount(apiClient)
  }
}

class GPlusSignIn(callback: GPlusTask ⇒ Unit)(implicit val act: Activity)
extends GPlusTask(callback)
{
  override def apiConnectionFailed(connectionResult: ConnectionResult) {
    act.startIntentSenderForResult(
      connectionResult.getResolution().getIntentSender(), GPlusBase.RC_SIGN_IN,
      null, 0, 0, 0
    )
  }
}

class GPlusBase
extends res.ResourcesAccess
{
  class Account(plus: GPlus)(implicit val context: Context)
  extends Basic
  {
    def photoUrl = plus.photo map { c ⇒ new URL(c.getUrl) }

    def coverUrl = plus.cover map { c ⇒ new URL(c.getUrl) }

    def drawable(url: Option[URL], callback: Drawable ⇒ Unit) {
      url foreach { u ⇒
        thread {
          val stream = u.openConnection().getInputStream()
          callback(Drawable.createFromStream(stream, ""))
        }
      }
    }

    def withPhoto(callback: Drawable ⇒ Unit) {
      drawable(photoUrl, callback)
    }

    def withCover(callback: Drawable ⇒ Unit) {
      drawable(coverUrl, callback)
    }

    def name = plus.name

    def email = plus.email
  }

  type PlusCallback[A] = (Account) ⇒ A
  type PlusJob = (GPlus) ⇒ Unit

  val scheduled: Buffer[PlusJob] = Buffer()

  val signedIn = rx.Var(false)

  var signInTask: Option[GPlusSignIn] = None

  def signIn()(implicit act: Activity) {
    if (signInTask.isEmpty) {
      signInImpl()
    }
  }

  private def signInImpl()(implicit act: Activity) {
    Try {
      signInTask = Some(new GPlusSignIn(t ⇒ signInComplete(true)))
      signInTask foreach { _.connect() }
    } recover {
      case e if TrypEnv.debug ⇒ throw e
    }
  }

  def signOut()(implicit c: Context) {
    Try {
      (new GPlusSignOut).connect()
    } recover {
      case e if TrypEnv.debug ⇒ throw e
    }
  }

  def signInComplete(success: Boolean)(implicit c: Context) {
    signedIn() = success
    signInTask = None
    if (success) {
      scheduled foreach(withAccount)
      scheduled.clear()
    }
  }

  def apply[A](callback: PlusCallback[A])(implicit a: Activity) = {
    val promise = Promise[A]()
    val job: PlusJob = { plus ⇒
      Future {
        Try(callback(new Account(plus))) match {
          case Success(result) ⇒ promise success result
          case Failure(error) ⇒ promise failure error
        }
      }
    }
    if (signedIn()) {
      withAccount(job)
    }
    else {
      scheduled += job
      signIn()
    }
    promise
  }

  def withAccount(job: PlusJob)(implicit c: Context) {
    GPlusTask(job).connect()
  }
}

object GPlusBase
extends GPlusBase
{
  val RC_SIGN_IN = 0
  val RC_TOKEN_FAIL = 1
}
