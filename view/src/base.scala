package tryp
package droid
package view

import reflect.macros.whitebox

import android.content.Context
import android.view.{View, ViewGroup}

import iota._

trait IOT[+A]
{
  def create: Context ⇒ iota.IO[A]

  def >>=[B](f: A ⇒ iota.IO[B]): IOT[B]

  def reify()(implicit c: Context): IO[A] = {
    create(c)
  }

  def perform()(implicit c: Context): A = {
    reify().perform()
  }
}

case class IOTS[+A](create: Context ⇒ iota.IO[A])
extends IOT[A]
{
  def >>=[B](f: A ⇒ iota.IO[B]): IOT[B] = IOTS(create >=> f)
}

trait Views
{
  def w[A]: IOT[A] = macro ViewM.w[A]

  def l[A <: ViewGroup](vs: IOT[_ <: View]*): IOT[A] = macro ViewM.l[A]
}

trait ViewMBase
{
  val c: whitebox.Context

  import c.universe._

  val actx = tq"android.content.Context"

  def lImpl[A <: ViewGroup: c.WeakTypeTag, B[_] <: IOT[_]]
  (ctor: c.Expr[(Context ⇒ iota.IO[A]) ⇒ B[A]])
  (vs: Seq[c.Expr[IOT[_ <: View]]]): c.Expr[B[A]] = {
    c.Expr[B[A]] {
      val tp = weakTypeOf[A]
      q"""
      iota.c[$tp] {
        $ctor { (ctx: $actx) ⇒
          val ch = Seq(..$vs)
          iota.l[$tp](ch map(_.create(ctx)): _*)(ctx)
        }
      }
      """
    }
  }
}

class ViewM(val c: whitebox.Context)
extends ViewMBase
{
  import c.universe._

  def w[A: c.WeakTypeTag]: c.Expr[IOT[A]] = {
    c.Expr[IOT[A]] {
      q"""
      tryp.droid.view.IOTS(iota.w[${weakTypeOf[A]}](_))
      """
    }
  }

  def l[A <: ViewGroup: c.WeakTypeTag](vs: c.Expr[IOT[_ <: View]]*)
  : c.Expr[IOT[A]] = {
    lImpl(reify(IOTS.apply[A](_)))(vs)
  }
}
