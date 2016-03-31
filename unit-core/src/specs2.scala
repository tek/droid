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
{
  def activityClass: Class[A]
}
