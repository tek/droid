package tryp
package droid

import macroid.FullDsl._

trait MainView
extends ActivityBase
with Transitions
{
  self: FragmentManagement
  with Akkativity ⇒

  implicit val fm: FragmentManagement = self

  def setContentView(v: View)

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    mainActor
    initView
  }

  def initView = {
    setContentView(Ui.get(mainLayout))
  }

  def mainLayout = contentLayout

  def contentLayout: Ui[ViewGroup] = {
    attachRoot(FL(bgCol("main"))(l[FrameLayout]() <~ Id.content))
  }

  def loadFragment(fragment: Fragment) = {
    loadView(frag(fragment, Id.content))
  }

  def loadShowFragment[A <: SyncModel: ClassTag]
  (model: A, ctor: () ⇒ ShowFragment[A]) {
    loadView(showFrag(model, ctor, Id.content))
  }

  def loadView(view: Ui[View]) {
    transition(view)
    contentLoaded()
  }

  def contentLoaded() {}

  // This is the entry point for back actions, when the actual back key or
  // the drawer toggle had been pressed. Any manual back initiation should also
  // call this.
  // The main actor can have its own back stack, so it is asked first. If it
  // declines or the message cannot be dispatched, it is sent back here and
  // dispatched to back() below.
  override def onBackPressed() {
    mainActor ! Messages.Back()
  }

  def back() {
    canGoBack ? goBack() | super.onBackPressed()
  }

  def goBack() {
    popBackStackSync
  }

  def canGoBack = backStackNonEmpty

  lazy val mainActor = createActor(MainActor.props)._2

  def showDetails(data: Model) {}
}
