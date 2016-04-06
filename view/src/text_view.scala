package tryp
package droid
package view

import scalaz._, Scalaz._, stream._, concurrent._

final class TextViewOps[A <: TextView, C](a: StreamIO[A, C])
{
  def text = a.view
    .map(_.getText.toString)
    .toSignal
}

trait ToTextViewOps
{
  implicit def ToTextViewOps[A <: TextView, C](a: StreamIO[A, C]) = 
    new TextViewOps(a)
}
