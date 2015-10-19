package tryp.debug

case class ProguardCacheError(source: Throwable)
extends Throwable(source)
