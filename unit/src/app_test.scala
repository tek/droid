package tryp
package droid
package test

import android.support.v7.widget.RecyclerView

import org.robolectric.Robolectric
import org.robolectric.shadows.ShadowApplication

trait TrypUnitSpec[A <: Activity with TrypTestActivity]
extends TrypSpecExt
with HasContext
with TrypDroidSpec
{
  def activityClass: Class[A]

  lazy val activityCtrl = Robolectric.buildActivity(activityClass)

  lazy val activity = activityCtrl.setup().get()

  implicit def context = ShadowApplication.getInstance.getApplicationContext

  override def assertion(isTrue: ⇒ Boolean, message: ⇒ String) =
    assert(isTrue, message)
}

trait TrypSpecExt
extends TrivialImplTestHelpers
{
  override def sleepCycle() = sync()

  def sync() = {
    Thread.sleep(100L)
    Robolectric.flushBackgroundThreadScheduler()
    Robolectric.flushForegroundThreadScheduler()
    Thread.sleep(100L)
  }

  implicit class SearchableExt(target: Searchable) {
    def recycler = {
      target.viewOfType[RecyclerView] effect { r ⇒
        sync()
        r.measure(0, 0)
        r.layout(0, 0, 100, 10000)
      }
    }

    def nonEmptyRecycler(count: Long) = {
      assertWM {
        recycler
          .map(r ⇒ s"recycler childcount ${r.getChildCount} != $count")
          .getOrElse("recycler doesn't exist")
      } { recycler exists { _.getChildCount == count } }
      recycler
    }
  }
}
