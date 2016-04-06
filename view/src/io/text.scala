package tryp
package droid
package view
package io

import android.util.TypedValue
import android.widget._
import android.text.{TextWatcher,TextUtils,Editable}

import iota.std.{TextCombinators => IText}

import core._

import annotation._

package object text
extends TextCombinators

abstract class TextCombinators
extends Combinators[TextView]
{
  def text(content: String) = k(_.setText(content))

  @contextwrapfold def hintR(resName: String) = {
    res.s(resName, Some("hint"))
      .map(text)
  }

  def size(points: Int) =
    k(_.setTextSize(TypedValue.COMPLEX_UNIT_SP, points))

  def medium = size(18)

  def large = size(22)

  @contextfold def minWidth(name: String) = {
    res.d(name, Some("min_width"))
      .map(_.toInt)
      .map(w => (_: TextView).setMinWidth(w))
  }

  def textWatcher(listener: TextWatcher) =
    ksub((t: EditText) => t.addTextChangedListener(listener))

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
