package tryp.view

import android.util.AttributeSet
import android.content.res.TypedArray
import android.content.res.ColorStateList

class TrypTextView(context: Context, attrs: AttributeSet, defStyle: Int)
extends android.widget.TextView(context, attrs, defStyle)
{
  def this(context: Context) = this(context, null, 0)
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)

  private var shadowColors: ColorStateList = null
  private var shadowDx: Float = 0
  private var shadowDy: Float = 0
  private var shadowRadius: Float = 0

  def updateShadowColor {
    if (shadowColors != null) {
      setShadowLayer(shadowRadius, shadowDx, shadowDy,
        shadowColors.getColorForState(getDrawableState, 0))
      invalidate
    }
  }

  override protected def drawableStateChanged {
    super.drawableStateChanged
    updateShadowColor
  }

  def setShadow(color: ColorStateList, radius: Double, x: Int = 0, y: Int = 0)
  {
    shadowColors = color
    shadowRadius = radius.toFloat
    shadowDx = x
    shadowDy = y
  }
}

object TrypTextView
{
  val TAG = "TrypTextView"
}
