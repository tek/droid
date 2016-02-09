package tryp
package droid
package view

object FixedIOPool
extends FixedPool
{
  def name = "io"

  val threads = 20
}

trait FixedIOStrategy
extends ExecutionStrategy
{
  def pool = FixedIOPool
}

object BoundedCachedIOPool
extends BoundedCachedPool
{
  def name = "io"

  override def maxThreads = 3
}

trait BoundedCachedIOStrategy
extends ExecutionStrategy
{
  def pool = BoundedCachedIOPool
}

trait IOStrategy
extends BoundedCachedIOStrategy
