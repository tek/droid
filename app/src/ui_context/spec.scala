package tryp.droid

import scalaz._, Scalaz._, concurrent._

import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.auth.GoogleAuthUtil

trait WithContext
extends AndroidUiContext
{
  def context[A](cb: Context => A): Option[A]
  def contextO: Option[Context]
}

trait DefaultWithContext
extends AndroidContextUiContext
with WithContext
{
  def context[A](cb: Context => A) = Some(cb(context))
  def contextO = aContext.some
}

object WithContext
{
  implicit def default(implicit c: Context) =
    new DefaultWithContext {
      def context = c
    }
}

trait StartActivity
extends AndroidUiContext
with WithContext
{
  def startActivity(intent: Intent, code: Int): Unit
  def resolveResult(result: ConnectionResult, code: Int): Unit
}

trait DefaultStartActivity
extends HasActivity
with StartActivity
with DefaultWithContext
{
  def startActivity(intent: Intent, code: Int) = {
    activity.startActivityForResult(intent, code)
  }

  def resolveResult(result: ConnectionResult, code: Int) = {
    result.startResolutionForResult(activity, code)
  }
}

object StartActivity
{
  implicit def default(implicit a: Activity) =
    new AndroidActivityUiContext with DefaultStartActivity {
      def activity = a
    }
}

trait AuthStateUiI
extends StartActivity
with AndroidUiContext
{
  def plusToken(email: String, scope: String): String
  def clearPlusToken(token: String): Unit
}

trait AuthStateUiContext
extends DefaultStartActivity
with AndroidActivityUiContext
with AuthStateUiI
{
  def plusToken(email: String, scope: String): String =
    GoogleAuthUtil.getToken(activity, email, scope)

  def clearPlusToken(token: String) = GoogleAuthUtil.clearToken(context, token)
}

object AuthStateUiI
{
  implicit def default(implicit a: Activity) =
    new AuthStateUiContext {
      def activity = a
    }
}
