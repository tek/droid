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
extends CKCombinators[TextView, StreamIO]
{
  @context def setText(content: String) = _.setText(content)
}

abstract class IOSpecBase[I[_, _]: ConsIO: ApplyKestrel]
(implicit M: Monad[I[?, Context]])
extends Spec
with Views[Context, I]
{
  def is = s2"""
  test $test
  """

  def test = {
    val lo = l[C](w[B], w[A], l[C](w[B], w[A]))
    def ok[A] = K((a: A) => ConsIO[I].pure[A, Context](ctx => a))
    lo >>- ok
    iota.c[ViewGroup] { lo >>= iota.lp(MATCH_PARENT, MATCH_PARENT) }
    val kest = iota.kestrel((_: FrameLayout).setForeground(null))
    val vgk: Kestrel[View, Context, I] =
      K((a: View) => ConsIO[I].pure[View, Context](c => a))
    val rl = l[FrameLayout](lo)
    val rl2 = rl >>- vgk >>= kest
    w[TextView] >>= text("")
    1 === 1
  }
}

class IOSpec
extends IOSpecBase[IO]

class StreamIOSpec
extends IOSpecBase[StreamIO]
