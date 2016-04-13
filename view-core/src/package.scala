package tryp
package droid
package view
package core

@exportNames(IO, Kestrel)
trait Exports
extends droid.core.Exports
with droid.core.Decls
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
with ToCKIotaKestrelOps

@integrate(droid.core, droid.core.Decls)
object `package`
extends droid.core.All
with IotaOrphans
{
  type CK[A] = Kestrel[A, Context, IO]
}
