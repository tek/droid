package tryp.droid.test

import reflect.classTag

import android.support.v7.widget.RecyclerView

import org.robolectric.Robolectric

import org.scalatest._
import matchers._

import tryp.droid._
import tryp.core._

abstract class TrypTest
extends FeatureSpec
with RobolectricSuite
with Matchers
with BeforeAndAfterEach
with BeforeAndAfterAll
with LoneElement
with TrypTestExt
with tryp.droid.HasContext
with TrypSpec
{
  Env.unit = true
  Env.test = true

  def waitFor(timeout: Long)(pred: ⇒ Boolean) {
    val start = Time.millis
    while (!pred && Time.millis - start < timeout) {
      Robolectric.runUiThreadTasksIncludingDelayedTasks()
      Thread.sleep(200L)
    }
  }

  def timeoutAssertion(isTrue: Boolean) = assert(isTrue,
    "Timeout waiting for predicate")
}

trait TrypTestExt
extends Matchers
{ this: TrypSpec ⇒

  implicit class `Option with assertion`[A: ClassTag](o: Option[A]) {
    o should bePresent[A]

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

  implicit class SearchableExt(target: Searchable) {
    def recycler = {
      target.viewOfType[RecyclerView] tap { r ⇒
        Robolectric.runUiThreadTasksIncludingDelayedTasks()
        r.measure(0, 0)
        r.layout(0, 0, 100, 10000)
      }
    }

    def nonEmptyRecycler = {
      assertW { recycler exists { _.getChildCount > 0 } }
      recycler
    }
  }

  class TrypOptionMatcher[A: ClassTag]
  extends Matcher[Option[A]]
  {

    def apply(o: Option[A]) = {
      val tp = classTag[A].className
      MatchResult(
        o.isDefined,
        s"Element of type ${tp} not present",
        s"Element of type ${tp} present"
      )
    }
  }

  def bePresent[A: ClassTag] = new TrypOptionMatcher[A]
}
