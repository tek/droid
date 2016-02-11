package tryp
package droid
package unit

import ZS._

import org.specs2._, specification._, matcher._, org.specs2.concurrent._

import android.support.v7.widget.RecyclerView

import org.robolectric.Robolectric

case class ContainsViewMatcher[A: RootView, B <: View: ClassTag]
(checker: ViewChecker[B])
extends Matcher[A]
{
  def sync() = {
    Thread.sleep(100L)
    Robolectric.flushBackgroundThreadScheduler()
    Robolectric.flushForegroundThreadScheduler()
    Thread.sleep(100L)
  }

  def apply[C <: A](e: Expectable[C]): MatchResult[C] = {
    sync()
    (e.value: A).viewOfType[B] map(checker.post) match {
      case Some(v) ⇒
        val (success, ok, ko) = checker.check(v)
        result(success, ok, ko, e)
      case None ⇒
        failure(s"does not contain ${className[B]}", e)
    }
  }
}

final class ContainsViewExpectable[A: RootView](a: ⇒ A)
extends TrypExpectable(() ⇒ a)
with ValueChecksBase
{
  def contains[B <: View: ClassTag](m: ⇒ ViewChecker[B]) = {
    applyMatcher(ContainsViewMatcher(m))
  }

  def willContain[B <: View: ClassTag](m: ⇒ ViewChecker[B]) = {
    applyMatcher(ContainsViewMatcher(m).eventually)
  }
}

abstract class ViewChecker[A <: View]
{
  def post(a: A): A

  def check(a: A): (Boolean, String, String)
}

class TypedViewMatcher[A <: View: ClassTag]
extends ViewChecker[A]
{
  def post(a: A) = a

  def check(a: A) = {
    (true, s"contains ${className[A]}", "")
  }
}

class RecyclerViewMatcher(count: Int)
extends ViewChecker[RecyclerView]
{
  def post(rec: RecyclerView) = {
    rec.measure(0, 0)
    rec.layout(0, 0, 100, 10000)
    rec
  }

  def check(rec: RecyclerView) = {
    (rec.getChildCount == count,
      s"Recycler has $count children",
      s"Recycler child count ${rec.getChildCount} != $count")
  }
}

trait ContainsView
extends SpecificationLike
{
  def nonEmptyRecycler(count: Int) = new RecyclerViewMatcher(count)

  def emptyRecycler = new RecyclerViewMatcher(0)

  def view[A <: View: ClassTag] = new TypedViewMatcher[A]

  implicit def anyToContainsViewExpectable[A: RootView](a: ⇒ A) =
    new ContainsViewExpectable[A](a)
}
