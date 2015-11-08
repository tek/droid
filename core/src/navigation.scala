package tryp.droid

import scalaz._, Scalaz._

import rx._

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

class ShowNavigationTarget[A <: SyncModel: ClassTag](title: String,
  fragment: () ⇒ ShowFragment[A], model: A)
extends NavigationTarget(title, fragment)
{
  override def create(id: Id)(implicit a: Activity, fm: FragmentManagement) =
    showFrag(model, fragment, id)
}

case class Navigation(drawerItems: NonEmptyList[DrawerItem])
{
  val current: Var[Maybe[NavigationTarget]] = Var(Maybe.Empty())

  lazy val targets = drawerItems.toList collect {
    case n: NavigationTarget ⇒ n
  }

  def isCurrent(target: NavigationTarget) = current().exists(_ == target)

  def ++(add: NonEmptyList[DrawerItem]) =
    copy(drawerItems = drawerItems append add)

  def to(target: NavigationTarget) = current() = target.just

  def apply(i: Int) = targets lift(i)
}

object Navigation
{
  def simple(first: DrawerItem, items: DrawerItem*) =
    Navigation(NonEmptyList(first, items: _*))

  def gplus(items: DrawerItem*) =
    Navigation.simple(GPlusHeader(), items: _*)
}

case class GPlusHeader()
extends DrawerItem
{
  def title = "GPlus"
}

case class DrawerButton(title: String, action: ViewState.Message)
extends DrawerItem
