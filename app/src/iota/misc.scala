package tryp
package droid
package io

import android.widget._

import cats.data._

import iota._

import view._

package object misc
extends IotaCombinators[android.view.View]
{
  def bgCol[A <: View](name: String)(implicit res: Resources): CK[A] = {
    resK(res.c(name, Some("bg")))(
      col => (_: View).setBackgroundColor(col.toInt))
  }

  def meta[A <: View](data: ViewMetadata)(implicit res: Resources) =
    kk[A, Unit](_.storeMeta(data).unsafePerformIO)

  def metaName[A <: View](name: String)(implicit res: Resources) =
    meta[A](SimpleViewMetadata(name))
}
