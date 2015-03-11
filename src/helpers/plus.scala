package tryp.droid

import java.net.URL

import android.graphics.drawable.Drawable

import com.google.android.gms.common._
import com.google.android.gms.plus._

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
      connectionResult.getResolution().getIntentSender(), GPlus.RC_SIGN_IN,
      null, 0, 0, 0
    )
  }
}

object GPlus
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

    def withImage(callback: Drawable ⇒ Unit) {
      drawable(photoUrl, callback)
    }

    def withCover(callback: Drawable ⇒ Unit) {
      drawable(coverUrl, callback)
    }

    def name = plus.name

    def email = plus.email
  }

  val scheduled: Buffer[(Account) ⇒ Unit] = Buffer()

  val RC_SIGN_IN = 0

  var signedIn = false

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
      case e if Env.debug ⇒ throw e
    }
  }

  def signOut()(implicit c: Context) {
    Try {
      (new GPlusSignOut).connect()
    } recover {
      case e if Env.debug ⇒ throw e
    }
  }

  def signInComplete(success: Boolean)(implicit c: Context) {
    signedIn = success
    signInTask = None
    if (success) {
      scheduled foreach(withAccount)
      scheduled.clear()
    }
  }

  def apply(callback: (Account) ⇒ Unit)(implicit a: Activity) {
    if (signedIn) {
      withAccount(callback)
    }
    else {
      scheduled += callback
      signIn()
    }
  }

  def withAccount(callback: (Account) ⇒ Unit)(implicit c: Context) {
    val task = GPlusTask { plus ⇒
      val account = new Account(plus)
      callback(account)
    }
    task.connect()
  }
}
