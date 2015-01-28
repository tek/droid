package tryp.droid.debug

case class ProguardCacheError(source: Throwable)
extends Throwable(source)
