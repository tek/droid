package tryp
package droid

object FixedStatePool
extends FixedPool
{
  val threads = 20
}

trait FixedStateStrategy
extends FixedStrategy
{
  def fixedPool = FixedStatePool
}

object CachedStatePool
extends CachedPool

trait CachedStateStrategy
extends CachedStrategy
{
  def cachedPool = CachedStatePool
}

object BoundedCachedStatePool
extends BoundedCachedPool
{
  override def maxThreads = 3
}

trait BoundedCachedStateStrategy
extends BoundedCachedStrategy
{
  def cachedPool = BoundedCachedStatePool
}

trait StateStrategy
extends BoundedCachedStateStrategy
