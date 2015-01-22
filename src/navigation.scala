package tryp.droid

case class NavigationTarget(title: String, fragment: () ⇒ Fragment,
  home: Boolean = false)

class Navigation(initial: NavigationTarget*)
{
  val targets: Buffer[NavigationTarget] = initial.toBuffer

  def drawerItems = targets

  var current: Option[NavigationTarget] = None

  def isCurrent(target: NavigationTarget) = current.contains(target)

  def ++=(addition: Seq[NavigationTarget]) = {
    targets ++= addition
    this
  }
}

object Navigation
{
  def apply(initial: NavigationTarget*) = new Navigation(initial: _*)
}
