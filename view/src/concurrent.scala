package tryp
package droid
package view

object IOPool
extends FixedPool
{
  val threads = 20
}

trait IOStrategy
extends FixedStrategy
{
  def fixedPool = IOPool
}
