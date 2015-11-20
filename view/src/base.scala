package tryp
package droid

import scala.reflect.macros.whitebox

import android.content.Context
import android.view.{View, ViewGroup}

import iota._

case class IOT[+A](create: Context ⇒ iota.IO[A])
{
  def >>=[B](f: A ⇒ iota.IO[B]): IOT[B] = IOT(create >=> f)

  def reify()(implicit c: Context): IO[A] = {
    create(c)
  }

  def perform()(implicit c: Context): A = {
    reify().perform()
  }
}

trait Views
{
  def w[A]: IOT[A] = macro ViewM.w[A]

  def l[A <: ViewGroup](vs: IOT[_ <: View]*): IOT[A] = macro ViewM.l[A]
}

class ViewM(val c: whitebox.Context)
{
  import c.universe._

  val actx = tq"android.content.Context"

  def w[A: c.WeakTypeTag]: c.Expr[IOT[A]] = {
    c.Expr[IOT[A]] {
      q"""
      IOT(iota.w[${weakTypeOf[A]}](_))
      """
    }
  }

  def l[A <: ViewGroup: c.WeakTypeTag](vs: c.Expr[IOT[_ <: View]]*)
  : c.Expr[IOT[A]] = {
    c.Expr[IOT[A]] {
      q"""
      iota.c[${weakTypeOf[A]}] {
        IOT { (ctx: $actx) ⇒
          val ch = Seq(..$vs)
          iota.l[${weakTypeOf[A]}](ch map(_.create(ctx)): _*)(ctx)
        }
      }
      """
    }
  }
}
