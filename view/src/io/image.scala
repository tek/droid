package tryp
package droid
package view
package io

import android.widget.ImageView

import view.core.annotation.contextfold
import view.core.Combinators

package object image
extends ImageCombinators

abstract class ImageCombinators
extends Combinators[ImageView]
{
  def imageId(id: Int) = k(_.setImageResource(id))

  @contextfold def imageRes(name: String) = {
    res.theme.drawable(name)
      .map(d => (_: ImageView).setImageDrawable(d))
  }
}
