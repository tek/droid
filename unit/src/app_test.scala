package tryp.droid.test

import org.scalatest._

class TrypTest
extends FeatureSpec
with RobolectricSuite
with Matchers
with BeforeAndAfterEach
with BeforeAndAfterAll
with LoneElement
with TrypTestExt
{
  override protected def beforeAll() {
    tryp.droid.Env.test = true
  }
}

trait TrypTestExt
extends Matchers
{
  implicit class `Option with assertion`[A](o: Option[A]) {
    o should be('defined)

    def foreachA(f: A ⇒ Unit) {
      o foreach f
    }

    def flatMapA[B](f: A ⇒ Option[B]) = {
      o flatMap f
    }

    def mapA[B](f: A ⇒ B) = {
      o map f
    }
  }
}
