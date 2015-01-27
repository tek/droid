package tryp.droid

import tweaks.Layout.{frag,showFrag}

case class NavigationTarget(title: String, fragment: () ⇒ Fragment,
  home: Boolean = false)
{
  def create(id: Id)(implicit a: Activity) = frag(fragment(), id)
}

class ShowNavigationTarget[A <: TrypModel](title: String, fragment: () ⇒
    ShowFragment[A], model: A)
extends NavigationTarget(title, fragment)
{
  override def create(id: Id)(implicit a: Activity) =
    showFrag(model, fragment, id)
}

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
