package tryp
package droid

trait HasNavigation
extends MainView
{ self: FragmentManagement
  with Akkativity ⇒

  val navigation: Navigation

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    resumeNavigation()
  }

  def resumeNavigation() {
    if (history.isEmpty) navigateIndex(0)
    else history.lastOption foreach(loadNavTarget)
  }

  def navigateIndex(index: Int) {
    navigation.targets.lift(index) foreach(navigate)
  }

  def navigate(target: NavigationTarget) {
    if (!navigation.current.contains(target)) {
      if (!tryPopHome(target)) history = target :: history
      loadNavTarget(target)
    }
  }

  def loadNavTarget(target: NavigationTarget) {
    ui { loadView(target.create(Id.content)) }
    navigation.current = Some(target)
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

  override def goBack() {
    history = history.tail
    history.headOption foreach(loadNavTarget)
  }

  override def canGoBack = history.length > 1

  def navigated(target: NavigationTarget) {
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
