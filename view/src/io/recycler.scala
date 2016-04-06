package tryp
package droid
package view
package io

import android.widget._
import android.support.v7.widget._

import view.core._

object recycler
extends RecyclerCombinators

class RecyclerCombinators
extends Combinators[RecyclerView]
{
  import annotation._

  @context def noClipToPadding[A <: ViewGroup] = _.setClipToPadding(false)

  @context def recyclerAdapter(adapter: RecyclerView.Adapter[_]) =
    _.setAdapter(adapter)

  @contextwrapfold def rvPad = {
    res.d("header_height", None)
      .map(_.toInt)
      .map(h => iota.padding[View](top = h.toInt).ck)
  }

  @context def layoutManager(m: RecyclerView.LayoutManager) =
    _.setLayoutManager(m)

  @contextwrap def linear = layoutManager(new LinearLayoutManager(ctx))

  def stagger(
    count: Long, orientation: Int = StaggeredGridLayoutManager.VERTICAL) =
      layoutManager(new StaggeredGridLayoutManager(count.toInt, orientation))

  @contextwrap def grid(count: Long) =
    layoutManager(new GridLayoutManager(ctx, count.toInt))

  @context def divider = 
    _.addItemDecoration(new DividerItemDecoration(ctx, null))

  def dataChanged = k(_.getAdapter.notifyDataSetChanged)

  def onScroll(callback: (ViewGroup, Int) => Unit) = k { v =>
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

  @context def reverseLayout =
    _.getLayoutManager match {
      case m: LinearLayoutManager => m.setReverseLayout(true)
      case m => Log.e(s"Used reverseLayout on incompatible type ${m.className}")
    }

  // def onScrollActor(actor: ActorSelection) =
  //   onScroll((view, y) => actor ! Messages.Scrolled(view, y))

  // def scrollTop = ck { rv =>
  //   rv.scrollToPosition(rv.getAdapter.getItemCount - 1)
  // }

  // def parallaxScroller[A <: Principal](actor: ActorSelection)
  // (implicit res: Resources) = {
  //   onScrollActor(actor) >- noClipToPadding >- rvPad
  // }
}
