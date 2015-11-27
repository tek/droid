package tryp
package droid
package unit

import org.specs2._, specification._, matcher._

trait UnitSpecs2Spec[A <: Activity with UnitActivity]
extends RobolectricSpecification
with UnitSpec[A]
with BeforeEach
with slick.ActionExpectations
with MustThrownExpectations
