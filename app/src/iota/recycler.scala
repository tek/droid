package tryp
package droid

import android.widget._
import android.support.v7.widget._

import akka.actor.ActorSelection

import iota._

import view._

trait RecyclerCombinators
extends IotaCombinators[RecyclerView]
with ResourcesAccess
{
  def noClipToPadding = kestrel((_: ViewGroup).setClipToPadding(false))

  def recyclerAdapter(a: RecyclerView.Adapter[_]) =
    kestrel((_: RecyclerView).setAdapter(a))

  @ck def linear = _.setLayoutManager(new LinearLayoutManager(ctx))

  @ck def layoutManager(m: RecyclerView.LayoutManager) = _.setLayoutManager(m)

  // def stagger(
  //   count: Long, orientation: Int = StaggeredGridLayoutManager.VERTICAL
  // ) =
  //   layoutManager(new StaggeredGridLayoutManager(count.toInt, orientation))

  @ckw def grid(count: Long) =
    layoutManager(new GridLayoutManager(ctx, count.toInt))(ctx)

  // def divider = ck(_.addItemDecoration(new DividerItemDecoration(ctx, null)))

  // def dataChanged = ck(_.getAdapter.notifyDataSetChanged)

  def onScroll(callback: (ViewGroup, Int) ⇒ Unit) = k { v ⇒
    val listener = new RecyclerView.OnScrollListener {
      var height = 0

      override def onScrolled(view: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(view, dx, dy)
        height += dy
        callback(view, height)
      }
    }
    v.setOnScrollListener(listener)
  }

  def onScrollActor(actor: ActorSelection) =
    onScroll((view, y) ⇒ actor ! Messages.Scrolled(view, y))

  // def reverseLayout = ck {
  //   _.getLayoutManager match {
  //     case m: LinearLayoutManager ⇒ m.setReverseLayout(true)
  //     case m ⇒ Log.e(s"Used reverseLayout on incompatible type ${m.className}")
  //   }
  // }

  // def scrollTop = ck { rv ⇒
  //   rv.scrollToPosition(rv.getAdapter.getItemCount - 1)
  // }

  @ckw def rvPad = padding[Principal](top = res.dimen("header_height").toInt)

  def parallaxScroller(actor: ActorSelection) = {
    onScrollActor(actor) >>= noClipToPadding >>= rvPad
  }
}
