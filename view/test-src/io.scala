package tryp
package droid
package view

import android.widget._

import shapeless._

import iota._

class IOTest
extends SpecBase
with ExtViews
with view.meta.Exports
{
  def is = s2"""
  foo $foo
  """

  def foo = {
    val rl = l[FrameLayout](
      w[TextView] :: w[Spinner] :: HNil
    )
    val kest = kestrel[FrameLayout, Unit](_.setForeground(null))
    val vgk: CK[View] = ctx => kestrel[View, Unit](a => ())
    val ks = kest >>= vgk
    val rl2 = rl >>= ks
    val tv = iota.text[TextView]("")
    w[TextView] >>= tv
    1 === 1
  }
}
