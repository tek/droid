package tryp
package droid
package view

// import com.google.android.gms.auth.GoogleAuthUtil

import simulacrum._

@typeclass trait Auth[A]
{
  def plusToken(a: A)(email: String, scope: String): String
  def clearPlusToken(a: A)(token: String): Unit
}

trait AuthInstances
{
  implicit def instance_Auth_Context[A <: Context] =
    new Auth[A] {
      def plusToken(a: A)(email: String, scope: String): String =
        "deprecated warning prevention"
        // GoogleAuthUtil.getToken(a, email, scope)

      def clearPlusToken(a: A)(token: String) =
        ???
        // GoogleAuthUtil.clearToken(a, token)
    }
}

object Auth
extends AuthInstances
