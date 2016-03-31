package tryp
package droid
package view
package core
package unit

import android.widget._

import cats.data._

import annotation._

object kest
extends CKCombinators[View, IO]
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
    w[TextView] >>- setId(1)
    1 === 1
  }
}
