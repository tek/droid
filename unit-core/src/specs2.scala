package tryp
package droid
package unit

import org.specs2._, specification._, matcher._

trait UnitSpecs2Spec[A <: Activity]
extends RobolectricSpecification
with UnitSpec[A]
with BeforeEach
with slick.ToActionExpectable
with MustThrownExpectations
with ContainsView
{
  def activityClass: Class[A]
}
