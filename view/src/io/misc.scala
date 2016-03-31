package tryp
package droid
package view
package io

import android.widget._

import cats.data._

import core._
import annotation._

object misc
extends ViewCombinators[StreamIO]

abstract class ViewCombinators[F[_, _]: ConsIO]
extends CKCombinators[android.view.View, F]
with ToViewOps
{
  @context def bgCol[A <: View](name: String) = { (v: View) =>
    res.c(name, Some("bg"))
      .map(_.toInt)
      .map(v.setBackgroundColor)
  }

  @context def meta[A <: View: ClassTag](data: ViewMetadata) =
    _.storeMeta(data) !? "store view metadata"

  def metaName[A <: View: ClassTag](name: String): Kestrel[A, Context, F] = 
    meta[A](SimpleViewMetadata(name))
}
