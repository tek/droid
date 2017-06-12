package tryp
package droid
package view
package core

@exportTypes(AIOI, Kestrel, Views, Combinators, ViewCombinators, DescribeAIO)
trait Types
{
  type CAIO[A] = AIO[A, Context]
}

@exportNames(AIO, DescribedKestrel, PerformAIO)
trait Names

@export
trait Exports
extends droid.core.Exports
with Names
with Types
{
  type CK[A] = Kestrel[A, Context, AIO]
}

trait All
extends droid.core.All
with ChainKestrelInstances
with ChainKestrel.ToChainKestrelOps
with ApplyKestrel.ToApplyKestrelOps
with ConsAIO.ToConsAIOOps
with AIOOrphans
with PerformAIO.ToPerformAIOOps
with DescribeAIO.ToDescribeAIOOps
with ToCKIotaKestrelOps

@integrate(droid.core)
object `package`
extends IotaOrphans
{
  type CK[A] = Kestrel[A, Context, AIO]
}
