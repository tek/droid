package tryp.droid.test

import org.scalatest._

abstract class TrypTest
extends FeatureSpec
with RobolectricSuite
with Matchers
with BeforeAndAfterEach
with BeforeAndAfterAll
with LoneElement
with TrypTestExt
with tryp.droid.view.HasActivity
{
  Env.unit = true
  Env.test = true

  override protected def beforeAll() {
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
