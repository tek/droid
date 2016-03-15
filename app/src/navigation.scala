package tryp.droid

import scalaz._, Scalaz._

import rx._

import tweaks.Layout.{frag,showFrag}

sealed trait DrawerItem
{
  def title: String
}

case class NavigationTarget(title: String, fragment: () => Fragment,
  home: Boolean = false)
extends DrawerItem
{
  def create[A: FragmentManagement: HasActivityF](parent: A, id: RId) =
    frag(parent, fragment(), id, title)
}

class ShowNavigationTarget[A <: SyncModel: ClassTag](title: String,
  fragment: () => ShowFragment[A], model: A)
extends NavigationTarget(title, fragment)
{
  override def create[A: FragmentManagement: HasActivityF]
  (parent: A, id: RId) =
    showFrag(parent, model, fragment, id)
}

case class Navigation(drawerItems: NonEmptyList[DrawerItem])
{
  val current: Var[Maybe[NavigationTarget]] = Var(Maybe.Empty())

  lazy val targets = drawerItems.toList collect {
    case n: NavigationTarget => n
  }

  def isCurrent(target: NavigationTarget) = current().exists(_ == target)

  def ++(add: NonEmptyList[DrawerItem]) =
    copy(drawerItems = drawerItems append add)

  def to(target: NavigationTarget) = current() = target.just

  def toIndex(i: Int) = current() = targets.lift(i).toMaybe

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

case class DrawerButton(title: String, action: state.Message)
extends DrawerItem
