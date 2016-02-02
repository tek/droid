package tryp
package droid
package view

object FixedIOPool
extends FixedPool
{
  val threads = 20
}

trait FixedIOStrategy
extends FixedStrategy
{
  def fixedPool = FixedIOPool
}

object BoundedCachedIOPool
extends BoundedCachedPool
{
  override def maxThreads = 3
}

trait BoundedCachedIOStrategy
extends BoundedCachedStrategy
{
  def cachedPool = BoundedCachedIOPool
}

trait IOStrategy
extends BoundedCachedIOStrategy
