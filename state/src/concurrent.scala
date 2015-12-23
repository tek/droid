package tryp
package droid

object StatePool
extends FixedPool
{
  val threads = 20
}

trait StateStrategy
extends FixedStrategy
{
  def fixedPool = StatePool
}

// object StatePool
// extends CachedPool

// trait StateStrategy
// extends CachedStrategy
// {
//   def cachedPool = StatePool
// }
