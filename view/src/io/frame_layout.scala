package tryp
package droid
package view
package io

import android.widget._
import android.support.v7.widget._
import android.graphics.drawable.Drawable

import view.core._

import cats._
import cats.syntax.foldable._

package object frame
extends FrameLayoutCombinators[StreamIO]

abstract class FrameLayoutCombinators[F[_, _]: ConsIO]
extends CKCombinators[FrameLayout, F]
{
  import annotation._

  def foreground(draw: Drawable) = 
    kp(_.setForeground(draw))

  @contextfold def selectableFg = {
    res.theme.drawable("selectableItemBackground")
      .map(foreground)
  }
}
