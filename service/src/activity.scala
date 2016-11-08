package tryp
package droid
package service

import simulacrum._

import com.google.android.gms.common.ConnectionResult

import core._

@typeclass trait ResolveResult[A]
{
  def resolveResult(a: A)(result: ConnectionResult, code: Int): Unit
}

trait ResolveResultInstances
{
  implicit def instance_ResolveResult_Activity[A <: Activity] =
    new ResolveResult[A] {
      def resolveResult(a: A)(result: ConnectionResult, code: Int) = {
        result.startResolutionForResult(a, code)
      }
    }
}

object ResolveResult
extends ResolveResultInstances
