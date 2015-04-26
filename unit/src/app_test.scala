package tryp.droid.test

import org.scalatest._

import tryp.droid._

abstract class TrypTest
extends FeatureSpec
with RobolectricSuite
with Matchers
with BeforeAndAfterEach
with BeforeAndAfterAll
with LoneElement
with TrypTestExt
with tryp.droid.HasActivity
{
  Env.unit = true
  Env.test = true

  override protected def beforeAll() {
  }

  def waitFor(timeout: Int)(pred: ⇒ Boolean) {
    val start = Time.millis
    while (!pred && Time.millis - start < timeout) {
      Thread.sleep(200L)
    }
  }

  def wait(pred: ⇒ Boolean) {
    waitFor(5000)(pred)
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
