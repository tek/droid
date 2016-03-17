package tryp
package droid
package view

import android.widget._

import shapeless._

import iota._

import iota.std.TextCombinators.text

class IOTest
extends Spec
with ExtViews
with view.meta.Exports
{
  def is = s2"""
  building $building
  """

  def building = {
    val t = w[TextView]
    val rl = l[FrameLayout](w[TextView] :: w[Spinner] :: HNil)
    val kest = kestrel((_: FrameLayout).setForeground(null))
    val vgk: CK[View] = ctx => kestrel((a: View) => ())
    val ks = kest >>= vgk
    val rl2 = rl >>- ks
    w[TextView] >>= text("")
    1 === 1
  }
}
