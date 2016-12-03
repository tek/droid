package tryp
package droid
package view
package core

@exportTypes(IOI, Kestrel, Views, Combinators, ViewCombinators, DescribeIO)
trait Types

@exportNames(IO, DescribedKestrel, PerformIO)
trait Names

@export
trait Exports
extends droid.core.Exports
with Names
with Types
{
  type CK[A] = Kestrel[A, Context, IO]
}

trait All
extends droid.core.All
with ChainKestrelInstances
with ChainKestrel.ToChainKestrelOps
with ApplyKestrel.ToApplyKestrelOps
with ConsIO.ToConsIOOps
with IOOrphans
with PerformIO.ToPerformIOOps
with DescribeIO.ToDescribeIOOps
with ToCKIotaKestrelOps

@integrate(droid.core)
object `package`
extends IotaOrphans
{
  type CK[A] = Kestrel[A, Context, IO]
}
