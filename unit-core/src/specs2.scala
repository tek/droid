package tryp
package droid
package unit

import org.specs2._, specification._, matcher._, robo._

trait UnitSpecs2Spec[A <: Activity]
extends ClassSpec
with UnitSpec[A]
with BeforeEach
with slick.ToActionExpectable
with MustThrownExpectations
with ContainsView
with tryp.Matchers
with Logging
with BoundedCachedPool
{
  def name = "spec"

  def activityClass: Class[A]

  def p[A](o: A) = pr (o)
}
