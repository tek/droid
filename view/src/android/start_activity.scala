package tryp
package droid
package view

import simulacrum._

import core._

@typeclass trait StartActivity[A]
{
  def startActivityCls[B <: Activity](a: A)(cls: Class[B]): Unit
  def startActivity(a: A)(intent: Intent): Unit
}

@typeclass trait StartActivityForResult[A]
{
  def startActivityForResult(a: A)(intent: Intent, code: Int): Unit
}

trait StartActivityInstances
{
  implicit def instance_StartActivity_Context[A <: Context] =
    new StartActivity[A] {
      def startActivityCls[B <: Activity](a: A)(cls: Class[B]) = {
        a.startActivity(new Intent(a, cls))
      }

      def startActivity(a: A)(intent: Intent) = {
        a.startActivity(intent)
      }
    }
}

trait StartActivityForResultInstances
{
  implicit def instance_StartActivityForResult_Activity[A <: Activity] =
    new StartActivityForResult[A] {
      def startActivityForResult(a: A)(intent: Intent, code: Int) = {
        a.startActivityForResult(intent, code)
      }
    }
}

object StartActivity
extends StartActivityInstances

object StartActivityForResult
extends StartActivityForResultInstances
