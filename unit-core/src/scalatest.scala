package tryp
package droid
package test

import reflect.classTag

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
{
  class TrypOptionMatcher[A: ClassTag]
  extends Matcher[Option[A]]
  {
    def apply(o: Option[A]) = {
      val tp = classTag[A].className
      MatchResult(
        o.isDefined,
        s"Element of type $tp not present",
        s"Element of type $tp present"
      )
    }
  }

  def bePresent[A: ClassTag] = new TrypOptionMatcher[A]
}
