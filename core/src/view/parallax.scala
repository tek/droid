package tryp.view

import android.graphics.{Canvas,Rect}

class ParallaxHeader(val context: Context)
extends RelativeLayout(context)
with tryp.AppPreferences
{
  val scrollFactor = appPrefs.float("parallax_scroll_factor", 0.2f)
  var scroll = 0.0f
  def offset = scroll * scrollFactor()
  def clip = (scroll * (1.0f - scrollFactor())).toInt
  lazy val headerHeight = this.res.dimen("header_height")

  def set(y: Int) {
    scroll = y.toFloat
    update()
  }

  def update() {
    if (offset <= headerHeight) {
      setTranslationY(-offset)
      invalidate()
    }
  }

  override def dispatchDraw(canvas: Canvas) {
    canvas.clipRect(new Rect(getLeft, getTop, getRight, getBottom - clip))
    super.dispatchDraw(canvas)
  }
}
