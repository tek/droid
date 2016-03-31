package tryp
package droid
package view
package core

trait Decls
{
  type CK[A, F[_, _]] = Kestrel[A, Context, F]
}

trait Exports
extends droid.core.Exports
with droid.core.ExportDecls
with ChainKestrelInstances
with ChainKestrel.ToChainKestrelOps
with IOInstances
with ApplyKestrel.ToApplyKestrelOps
with PerformIO.ToPerformIOOps
with Decls

object `package`
extends droid.core.Exports
with droid.core.ExportDecls
with IOInstances
with IotaInstances
with Decls
