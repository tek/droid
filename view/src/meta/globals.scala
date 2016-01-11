package tryp
package droid
package view.meta

trait Globals
extends droid.core.meta.Globals
with IOTypes
with IOInstances

trait Exports
extends Globals
with view.BuilderOps
with view.IOBuilder.ToIOBuilderOps
with view.ChainKestrelInstances
with view.ChainKestrel.ToChainKestrelOps
