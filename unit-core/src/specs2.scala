package tryp
package droid
package test

import org.specs2._, specification._, matcher._

trait TrypUnitSpecs2Spec[A <: Activity with TrypTestActivity]
extends RobolectricSpecification
with TrypUnitSpec[A]
with BeforeEach
with slick.ActionExpectations
with MustThrownExpectations
