package tryp
package droid
package view.meta

trait Exports
{
  type IOT[+A] = view.IOT[A]

  type IOTS[+A] = view.IOTS[A]
  val IOTS = view.IOTS

  type IOB[A] = view.IOB[A]
  val IOB = view.IOB
}

trait Globals
extends droid.core.meta.Globals
with Exports
