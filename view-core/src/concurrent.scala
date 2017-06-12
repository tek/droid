package tryp
package droid
package view
package core

object FixedAIOPool
extends FixedPool
{
  def name = "io"

  val threads = 20
}

trait FixedAIOStrategy
extends ExecutionStrategy
{
  def pool = FixedAIOPool
}

object BoundedCachedAIOPool
extends BoundedCachedPool
{
  def name = "io"

  override def maxThreads = 3
}

trait BoundedCachedAIOStrategy
extends ExecutionStrategy
{
  def pool = BoundedCachedAIOPool
}

trait AIOStrategy
extends BoundedCachedAIOStrategy
