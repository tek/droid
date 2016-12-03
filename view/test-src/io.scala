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

class IOSpec
extends Spec
with Views[Context, IO]
{
  def is = s2"""
  test $test
  """

  def test = {
    val lo: IO[C, Context] = l[C](w[B], w[A], l[C](w[B], w[A]))
    def ok[A]: Kestrel[A, Context, IO] =
      K((a: A) => ConsIO[IO].pure[A, Context](ctx => a))
    lo >>- ok
    iota.c[ViewGroup] { lo >>= iota.lp(MATCH_PARENT, MATCH_PARENT) }
    val kest = iota.kestrel((_: FrameLayout).setForeground(null))
    val vgk = K((a: View) => ConsIO[IO].pure[View, Context](c => a))
    val rl = l[FrameLayout](lo)
    val mapped: IO[Unit, Context] = rl.map(a => ())
    val rl2 = rl >>- vgk >>= kest
    w[TextView] >>= text("")
    w[TextView] >>- kestrels.setText("")
    1 === 1
  }
}
