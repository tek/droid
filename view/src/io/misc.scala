package tryp
package droid
package view
package io

import android.widget._

import cats.data._

import core._
import annotation._

object misc
extends MiscCombinators

abstract class MiscCombinators
extends ViewCombinators
with ToViewOps
{
  @context def bgCol[A <: View](name: String) = { (v: View) =>
    res.c(name, Some("bg"))
      .map(_.toInt)
      .map(v.setBackgroundColor)
  }

  def meta[A <: View: ClassTag](data: ViewMetadata) =
    ksub((_: A).storeMeta(data))

  def metaName[A <: View: ClassTag](name: String) =
    meta[A](NamedVMD(className[A], name))

  def nopSub[A <: View] = super.nopKSub[A]

  def nop = super.nopK

  def fitsSystemWindows = k(_.setFitsSystemWindows(true))
}
