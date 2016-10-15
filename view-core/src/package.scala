package tryp
package droid
package view
package core

@exportNames(IO, Kestrel, Views, Combinators, ViewCombinators,
  DescribedKestrel, PerformIO)
trait Exports
extends droid.core.Exports
{
  type CK[A] = Kestrel[A, Context, IO]
  val IO = view.core.IO
  val DescribedKestrel = view.core.DescribedKestrel
}

trait All
extends droid.core.All
with ChainKestrelInstances
with ChainKestrel.ToChainKestrelOps
with ApplyKestrel.ToApplyKestrelOps
with ConsIO.ToConsIOOps
with IOOrphans
with PerformIO.ToPerformIOOps
with ToCKIotaKestrelOps

@integrate(droid.core)
object `package`
extends IotaOrphans
{
  type CK[A] = Kestrel[A, Context, IO]
}
