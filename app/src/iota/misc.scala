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
  @ck def bgCol[A <: View](name: String) = { (v: View) =>
    res.c(name, Some("bg"))
      .foreach(col => v.setBackgroundColor(col.toInt))
  }

  @ck def meta[A <: View](data: ViewMetadata) =
    _.storeMeta(data).unsafePerformIO

  def metaName[A <: View](name: String) = meta[A](SimpleViewMetadata(name))
}
