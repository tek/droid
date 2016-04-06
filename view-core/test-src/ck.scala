package tryp
package droid
package view
package core
package unit

import android.widget._

import annotation._

object kest
extends ViewCombinators
{
  @context def setId(id: Int) =
    _.setId(id)

  @contextfold def foldId(id: Int) =
    Xor.right(id).map(setId)

  @context def wrapId(id: String) =
    setId(ctx.getResources.getIdentifier(id, "", ""))

  @context def bgCol[A <: View](name: String) = { (v: View) =>
    this.res.c(name, Some("bg"))
      .foreach(col => v.setBackgroundColor(col.toInt))
  }
}

class CKSpec
extends Spec
with Views[Context, IO]
{
  def is = s2"""
  test $test
  """

  import kest._

  def test = {
    w[TextView] >>- setId(1) >>- foldId(1) >>- wrapId("foo") >>- bgCol("foo")
    1 === 1
  }
}
