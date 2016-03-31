package tryp
package droid
package state
package core

object FixedStatePool
extends FixedPool
{
  def name = "state"

  val threads = 20
}

trait FixedStateStrategy
extends ExecutionStrategy
{
  def pool = FixedStatePool
}

object CachedStatePool
extends CachedPool
{
  def name = "state"
}

trait CachedStateStrategy
extends ExecutionStrategy
{
  def pool = CachedStatePool
}

object BoundedCachedStatePool
extends BoundedCachedPool
{
  def name = "state"

  override def maxThreads = 10
}

trait BoundedCachedStateStrategy
extends ExecutionStrategy
{
  def pool = BoundedCachedStatePool
}

trait StateStrategy
extends BoundedCachedStateStrategy
