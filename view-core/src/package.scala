package tryp
package droid
package view
package core

@exportNames(IO, IOI, Kestrel, Views, Combinators, ViewCombinators,
  DescribedKestrel, PerformIO)
trait Exports
extends droid.core.Exports
{
  type CK[A] = Kestrel[A, Context, IO]
  val IO = droid.view.core.IO
  val DescribedKestrel = droid.view.core.DescribedKestrel
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
{
  def cio[A](f: Context => A): IO[A, Context] = IO[A, Context](f)
}

@integrate(droid.core)
object `package`
extends IotaOrphans
{
  type CK[A] = Kestrel[A, Context, IO]
}
