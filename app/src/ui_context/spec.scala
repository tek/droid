// package tryp.droid

// import com.google.android.gms.common.ConnectionResult
// import com.google.android.gms.auth.GoogleAuthUtil

// import view._
// import view.core._
// import state._
// import state._

// abstract class WithContext[F[_, _]: ConsIO]
// extends Android[F]
// {
//   def context[A](cb: Context => A): Option[A]
//   def contextO: Option[Context]
// }

// abstract class DefaultWithContext[F[_, _]: ConsIO]
// extends ContextAndroid[F]
// with WithContext[F]
// {
//   def context[A](cb: Context => A) = Some(cb(context))
//   def contextO = aContext.some
// }

// object WithContext
// {
//   implicit def default[F[_, _]: ConsIO](implicit c: Context) =
//     new DefaultWithContexttrait {
//       def context = c
//     }
// }

// abstract class StartActivity[F[_, _]: ConsIO]
// extends Android[F]
// with WithContext[F]
// {
//   def startActivity(intent: Intent, code: Int): Unit
//   def resolveResult(result: ConnectionResult, code: Int): Unit
// }

// abstract class DefaultStartActivity[F[_, _]: ConsIO]
// extends HasActivity[F]
// with StartActivity[F]
// with DefaultWithContext[F]
// {
//   def startActivity(intent: Intent, code: Int) = {
//     activity.startActivityForResult(intent, code)
//   }

//   def resolveResult(result: ConnectionResult, code: Int) = {
//     result.startResolutionForResult(activity, code)
//   }
// }

// object StartActivity
// {
//   implicit def default[F[_, _]: ConsIO](implicit a: Activity) =
//     new AndroidActivityUiContext[F] with DefaultStartActivity[F] {
//       def activity = a
//     }
// }

// abstract class AuthStateUiI[F[_, _]: ConsIO]
// extends StartActivity[F]
// with Android[F]
// {
//   def plusToken(email: String, scope: String): String
//   def clearPlusToken(token: String): Unit
// }

// abstract class AuthStateUiContext[F[_, _]: ConsIO]
// extends DefaultStartActivity[F]
// with AndroidActivityUiContext[F]
// with AuthStateUiI[F]
// {
//   def plusToken(email: String, scope: String): String =
//     GoogleAuthUtil.getToken(activity, email, scope)

//   def clearPlusToken(token: String) = GoogleAuthUtil.clearToken(context, token)
// }

// object AuthStateUiI
// {
//   implicit def default[F[_, _]: ConsIO](implicit a: Activity) =
//     new AuthStateUiContext[F] {
//       def activity = a
//     }
// }
