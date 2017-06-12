package tryp
package droid
package unit

import view.core.annotation._

class PerformCKMainSpec
extends UnitSpecs2Spec[Activity]
{
  def is = s2"""
  test $test
  """

  def before = ()

  def activityClass = classOf[Activity]

  def test = {
    case class Muto(var state: Int)
    val v1 = 9
    val v2 = 47
    val kest = { (a: Muto) => a.state = v2; Muto(v1 + v2) }
    val s = AIO.lift[Int](v => Muto(v)) >>- kest
    s.main()(v1) will_== Muto(v2)
  }
}

object kest
extends ViewCombinators
{
  @context def setId(id: Int) =
    _.setId(id)

  @contextwrap def wrapId(id: Int) =
    setId(id)

  @contextfold def foldId(id: Int) =
    Either.Right(id).map(i => (_: View).setId(i))

  @contextwrapfold def wrapFoldId(id: Int) =
    Either.Right(id).map(setId)
}

class CKSpec
extends ActivitySpec[Activity]
with Views[Context, AIO]
{
  import kest._

  def is = s2"""
  context kestrel annotations

  simple ${check(setId)}
  wrap ${check(wrapId)}
  fold ${check(foldId)}
  wrap fold ${check(wrapFoldId)}
  """

  def activityClass = classOf[Activity]

  val id = 23

  def check(f: Int => Kestrel[View, Context, AIO]) = {
    (w[TextView] >>- f(id)).unsafePerformAIO.map(_.getId) computes_== id
  }
}
