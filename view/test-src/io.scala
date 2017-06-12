package tryp
package droid
package view
package unit

import android.widget._
import android.view.ViewGroup.LayoutParams._

import iota.std.TextCombinators.text

import core._
import state._
import annotation._

class A(c: Context)
extends View(c)

class B(c: Context)
extends View(c)

class C(c: Context)
extends FrameLayout(c)

object kestrels
extends Combinators[TextView]
{
  @context def setText(content: String) = _.setText(content)
}

class AIOSpec
extends Spec
with Views[Context, AIO]
{
  def is = s2"""
  test $test
  """

  def test = {
    val lo: AIO[C, Context] = l[C](w[B], w[A], l[C](w[B], w[A]))
    def ok[A]: Kestrel[A, Context, AIO] =
      K((a: A) => ConsAIO[AIO].pure[A, Context](ctx => a))
    lo >>- ok
    iota.c[ViewGroup] { lo >>= iota.lp(MATCH_PARENT, MATCH_PARENT) }
    val kest = iota.kestrel((_: FrameLayout).setForeground(null))
    val vgk = K((a: View) => ConsAIO[AIO].pure[View, Context](c => a))
    val rl = l[FrameLayout](lo)
    val mapped: AIO[Unit, Context] = rl.map(a => ())
    val rl2 = rl >>- vgk >>= kest
    w[TextView] >>= text("")
    w[TextView] >>- kestrels.setText("")
    1 === 1
  }
}
