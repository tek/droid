package tryp.droid

case class NavigationTarget(title: String, fragment: () ⇒ Fragment,
  home: Boolean = false)

case class Navigation(targets: NavigationTarget*)
{
  def drawerItems = targets

  var current: Option[NavigationTarget] = None

  def isCurrent(target: NavigationTarget) = current.contains(target)
}
