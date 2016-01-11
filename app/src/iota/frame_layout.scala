package tryp
package droid

import android.widget._
import android.support.v7.widget._
import android.graphics.drawable.Drawable

import iota._

import view._

trait FrameLayoutCombinators
extends IotaCombinators[FrameLayout]
with ResourcesAccess
{
  def foreground(res: Drawable) = k(_.setForeground(res))
}
