package tryp
package droid
package view
package io

import android.widget._
import android.support.v7.widget._
import android.graphics.drawable.Drawable

import view.core._

object frame
extends FrameLayoutCombinators

abstract class FrameLayoutCombinators
extends Combinators[FrameLayout]
{
  import annotation._

  def foreground(draw: Drawable) = 
    k(_.setForeground(draw))

  @contextwrapfold def selectableFg = {
    res.theme.drawable("selectableItemBackground")
      .map(foreground)
  }
}
