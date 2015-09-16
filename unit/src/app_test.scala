package tryp.droid.test

import reflect.classTag

import android.support.v7.widget.RecyclerView

import org.robolectric.Robolectric

import org.scalatest._
import matchers._

import tryp.droid._
import tryp._

trait TrypUnitSpec
extends FeatureSpec
with RobolectricSuite
with Matchers
with BeforeAndAfterEach
with BeforeAndAfterAll
with LoneElement
with TrypSpecExt
with tryp.droid.HasContext
with TrypSpec
{
  def timeoutAssertion(isTrue: Boolean) = assert(isTrue,
    "Timeout waiting for predicate")
}

trait TrypSpecExt
extends Matchers
with TrivialImplTestHelpers
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

  override def sleepCycle() = sync()

  def sync() = {
    Thread.sleep(100L)
    Robolectric.flushForegroundThreadScheduler()
    Thread.sleep(100L)
  }

  implicit class SearchableExt(target: Searchable) {
    def recycler = {
      target.viewOfType[RecyclerView] tap { r ⇒
        sync()
        r.measure(0, 0)
        r.layout(0, 0, 100, 10000)
      }
    }

    def nonEmptyRecycler(count: Long) = {
      assertW { recycler exists { _.getChildCount == count } }
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
