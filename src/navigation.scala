package tryp.droid

import tweaks.Layout.{frag,showFrag}

sealed trait DrawerItem
{
  def title: String
}

case class NavigationTarget(title: String, fragment: () ⇒ Fragment,
  home: Boolean = false)
extends DrawerItem
{
  def create(id: Id)(implicit a: Activity, fm: FragmentManagement) =
    frag(fragment(), id, title)
}

class ShowNavigationTarget[A <: TrypModel: ClassTag](title: String,
  fragment: () ⇒ ShowFragment[A], model: A)
extends NavigationTarget(title, fragment)
{
  override def create(id: Id)(implicit a: Activity, fm: FragmentManagement) =
    showFrag(model, fragment, id)
}

class Navigation(initial: NavigationTarget*)
{
  val targets: Buffer[NavigationTarget] = initial.toBuffer

  def drawerItems: List[DrawerItem] = targets.toList

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

case class GPlusHeader()
extends DrawerItem
{
  def title = "GPlus"
}

class GPlusNavigation(initial: NavigationTarget*)
extends Navigation(initial: _*)
{
    override def drawerItems = GPlusHeader() :: super.drawerItems
}

object GPlusNavigation
{
  def apply(initial: NavigationTarget*) = new GPlusNavigation(initial: _*)
}
