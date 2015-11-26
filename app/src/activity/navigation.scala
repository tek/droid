package tryp
package droid

import NavMessages._

trait HasNavigation
extends MainView
with HasContextAgent
{ self: FragmentManagement
  with Akkativity ⇒

  def navigation: Navigation

  lazy val navMachine = new NavMachine {
    def handle = "nav"
  }

  override def machines = navMachine :: super.machines

  override def postRunMachines() = {
    send(SetNav(navigation))
  }

  def navigateIndex(index: Int) {
    send(Index(index))
  }

  def loadNavTarget(target: NavigationTarget) {
    send(MainViewMessages.LoadUi(target.create(Id.content)))
    navigated(target)
  }

  def tryPopHome(target: NavigationTarget) = {
    target.home && {
      clearHistory(target)
      history.headOption contains target
    }
  }

  def clearHistory(target: NavigationTarget) = {
    history = history drop(history indexOf target)
  }

  var history: List[NavigationTarget] = List()

  override def back() {
    send(Back)
  }

  def navigated(target: NavigationTarget) {
  }

  def navigate(target: NavigationTarget): Unit = {
    send(Target(target))
  }

  def loadFragment(name: String, ctor: () ⇒ Fragment) {
    val target = new NavigationTarget(name, ctor)
    navigate(target)
  }

  override def loadShowFragment[A <: SyncModel: ClassTag]
  (model: A, ctor: () ⇒ ShowFragment[A]) {
    val target = new ShowNavigationTarget("Details", ctor, model)
    navigate(target)
  }
}
