package tryp
package droid
package view
package core

trait Decls
{
  type CK[A] = Kestrel[A, Context, IO]
}

trait Exports
extends droid.core.Exports
with droid.core.ExportDecls
with ChainKestrelInstances
with ChainKestrel.ToChainKestrelOps
with ApplyKestrel.ToApplyKestrelOps
with IOOrphans
with PerformIO.ToPerformIOOps
with Decls

object `package`
extends droid.core.Exports
with droid.core.ExportDecls
with IotaOrphans
with Decls
