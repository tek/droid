package tryp
package droid
package view

import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet

class DividerItemDecoration
extends RecyclerView.ItemDecoration
{

  private var mDivider: Drawable = null

  private var mShowFirstDivider: Boolean = false

  private var mShowLastDivider: Boolean = false

  def this(context: Context, attrs: AttributeSet) {
    this()
    val a = context.obtainStyledAttributes(attrs, Array(android.R.attr.listDivider))
    mDivider = a.getDrawable(0)
    a.recycle()
  }

  def this(context: Context,
      attrs: AttributeSet,
      showFirstDivider: Boolean,
      showLastDivider: Boolean) {
    this(context, attrs)
    mShowFirstDivider = showFirstDivider
    mShowLastDivider = showLastDivider
  }

  def this(divider: Drawable) {
    this()
    mDivider = divider
  }

  def this(divider: Drawable, showFirstDivider: Boolean, showLastDivider: Boolean) {
    this(divider)
    mShowFirstDivider = showFirstDivider
    mShowLastDivider = showLastDivider
  }

  override def getItemOffsets(outRect: Rect,
      view: View,
      parent: RecyclerView,
      state: RecyclerView.State) {
    super.getItemOffsets(outRect, view, parent, state)
    if (mDivider == null) {
      return
    }
    if (parent.getChildLayoutPosition(view) < 1) {
      return
    }
    if (getOrientation(parent) == LinearLayoutManager.VERTICAL) {
      outRect.top = mDivider.getIntrinsicHeight
    } else {
      outRect.left = mDivider.getIntrinsicWidth
    }
  }

  override def onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
    if (mDivider == null) {
      super.onDrawOver(c, parent, state)
      return
    }
    var left = 0
    var right = 0
    var top = 0
    var bottom = 0
    var size: Int = 0
    val orientation = getOrientation(parent)
    val childCount = parent.getChildCount
    if (orientation == LinearLayoutManager.VERTICAL) {
      size = mDivider.getIntrinsicHeight
      left = parent.getPaddingLeft
      right = parent.getWidth - parent.getPaddingRight
    } else {
      size = mDivider.getIntrinsicWidth
      top = parent.getPaddingTop
      bottom = parent.getHeight - parent.getPaddingBottom
    }
    val startIndex = if (mShowFirstDivider) 0 else 1
    for (i <- startIndex until childCount) {
      val child = parent.getChildAt(i)
      val params = child.getLayoutParams.asInstanceOf[RecyclerView.LayoutParams]
      if (orientation == LinearLayoutManager.VERTICAL) {
        top = child.getTop - params.topMargin
        bottom = top + size
      } else {
        left = child.getLeft - params.leftMargin
        right = left + size
      }
      mDivider.setBounds(left, top, right, bottom)
      mDivider.draw(c)
    }
    if (mShowLastDivider && childCount > 0) {
      val child = parent.getChildAt(childCount - 1)
      val params = child.getLayoutParams.asInstanceOf[RecyclerView.LayoutParams]
      if (orientation == LinearLayoutManager.VERTICAL) {
        top = child.getBottom + params.bottomMargin
        bottom = top + size
      } else {
        left = child.getRight + params.rightMargin
        right = left + size
      }
      mDivider.setBounds(left, top, right, bottom)
      mDivider.draw(c)
    }
  }

  private def getOrientation(parent: RecyclerView): Int = {
    if (parent.getLayoutManager.isInstanceOf[LinearLayoutManager]) {
      val layoutManager = parent.getLayoutManager.asInstanceOf[LinearLayoutManager]
      layoutManager.getOrientation
    } else {
      throw new IllegalStateException("DividerItemDecoration can only be used with a LinearLayoutManager.")
    }
  }
}
