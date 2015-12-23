package tryp
package droid
package view

import scalaz._, Scalaz._, stream._, concurrent._

import android.widget.TextView

final class TextViewOps[A <: TextView](a: IOB[A])
{
  def text = a.view
    .map(_.getText.toString)
    .toSignal
}

trait ToTextViewOps
{
  implicit def ToTextViewOps[A <: TextView](a: IOB[A]) = new TextViewOps(a)
}
