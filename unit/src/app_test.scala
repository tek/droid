package tryp.droid.test

import org.scalatest._

class AppTest
extends FeatureSpec
with RobolectricSuite
with Matchers
with BeforeAndAfter
with BeforeAndAfterAll
with LoneElement
{
  override protected def beforeAll() {
    tryp.droid.Env.test = true
  }
}
