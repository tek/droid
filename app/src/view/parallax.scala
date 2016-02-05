package tryp
package droid

import android.graphics.{Canvas, Rect}

import iota._

class ParallaxHeader(c: Context)
extends RelativeLayout(c)
with AppPreferences
with ResourcesAccess
{
  implicit def context = c

  val scrollFactor = appPrefs.float("parallax_scroll_factor", 0.2f)
  var scroll = 0.0f
  def offset = scroll * scrollFactor()
  def clip = (scroll * (1.0f - scrollFactor())).toInt
  lazy val headerHeight: Float = res.dimen("header_height").getOrElse(200.dp)

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
