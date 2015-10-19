package tryp
package test

import org.scalatest._
import matchers._

trait TrypUnitScalatestSpec[A <: Activity with TrypTestActivity]
extends FeatureSpec
with RobolectricSuite
with TrypUnitSpec[A]
with Matchers
with BeforeAndAfterEach
with BeforeAndAfterAll
with LoneElement
with concurrent.ScalaFutures
