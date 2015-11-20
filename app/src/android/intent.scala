package tryp
package droid

final class IntentOps(intent: Intent)
{
  implicit val mm: Monoid[Params] = scalaz.std.map.mapMonoid

  def extrasSafe: Params = {
    Option(intent) flatMap(i ⇒ Option(i.getExtras)) map(_.toMap) orZero
  }

  def storeExtras(extras: Params) = {
    extras foreach { case (k, v) ⇒ intent.putExtra(k, v) }
  }
}

trait ToIntentOps
{
  implicit def ToIntentOps(intent: Intent) = new IntentOps(intent)
}
