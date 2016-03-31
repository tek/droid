package tryp
package droid
package view
package io

import android.util.TypedValue
import android.widget._
import android.text.{TextWatcher,TextUtils,Editable}

import iota._
import iota.std.{TextCombinators => IText}

import core._

package object text
extends TextCombinators[StreamIO]

abstract class TextCombinators[F[_, _]: ConsIO]
extends IotaCombinators[TextView, F]
{
  def hintR[A <: TextView](resName: String)(implicit res: Resources) = {
    res.s(resName, Some("hint"))
      .map(IText.hint)
      .getOrElse(nopK)
  }

  def size[A <: TextView](points: Int): iota.Kestrel[A] = {
    kestrel(_.setTextSize(TypedValue.COMPLEX_UNIT_SP, points))
  }

  def medium[A <: TextView]: iota.Kestrel[A] = size(18)

  def large[A <: TextView]: iota.Kestrel[A] = size(22)

  def minWidth[A <: TextView](name: String)(implicit res: Resources) = {
    res.d(name, Some("min_width"))
      .map(w => kestrel((_: A).setMinWidth(w.toInt)))
      .getOrElse(nopK)
  }

  def textWatcher(listener: TextWatcher) = {
    kestrel((_: EditText).addTextChangedListener(listener))
  }

  def watchText(cb: => Unit) = {
    val listener = new TextWatcher {
      def onTextChanged(cs: CharSequence, start: Int, count: Int, after: Int) {
        cb
      }

      def beforeTextChanged(cs: CharSequence, start: Int, count: Int, after:
        Int) { }

      def afterTextChanged(edit: Editable) { }
    }
    textWatcher(listener)
  }

  def clear[A <: TextView] = IText.text[A]("")
}
