package tryp
package droid

import android.util.TypedValue
import android.widget._
import android.text.{TextWatcher,TextUtils,Editable}

import iota._

trait TextCombinators
{
  def hintR[A <: TextView](resName: String)(implicit res: Resources) = {
    hint(res.s(resName, Some("hint")))
  }

  def size(points: Int) = {
    kestrel((_: TextView).setTextSize(TypedValue.COMPLEX_UNIT_SP, points))
  }
  val medium = size(18)
  val large = size(22)

  def minWidth(name: String)(implicit res: Resources) = {
    val width = res.d(name, Some("min_width")).toInt
    kestrel((_: TextView).setMinWidth(width))
  }

  def textWatcher(listener: TextWatcher) = {
    kestrel((_: EditText).addTextChangedListener(listener))
  }

  def watchText(cb: ⇒ Unit) = {
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
}
