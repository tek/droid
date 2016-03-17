package tryp
package droid

import android.widget._
import android.support.v7.widget._
import android.graphics.drawable.Drawable

import iota._

import view._

import cats._
import cats.syntax.foldable._

trait FrameLayoutCombinators
extends IotaCombinators[FrameLayout]
with ResourcesAccess
{
  def foreground(draw: Drawable): CK[FrameLayout] = kk(_.setForeground(draw))

  @ckwf def selectableFg = {
    res.theme.drawable("selectableItemBackground")
      .map(foreground)
  }
}
