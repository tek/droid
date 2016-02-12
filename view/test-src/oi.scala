package tryp
package droid
package unit

import state._
import view._

import org.specs2._

class A(c: Context)
class B(c: Context)
class C(c: Context)

class OISpec
extends Specification
with tryp.Matchers
with OIViews[Context]
{
  def is = s2"""
  test $test
  """

  def test = {
    val lo = l[A](w[B], w[C], l[C](w[B], w[A]))
    1 === 1
  }
}
