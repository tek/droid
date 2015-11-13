package tryp
package droid

import macroid.FullDsl._

import ViewState._
import MainViewMessages._

trait MainView
extends ActivityBase
with Transitions
with Stateful
{
  mainView: FragmentManagement
  with Akkativity ⇒

  val content = slut[FrameLayout]

  lazy val mainViewImpl = new MainViewImpl {
    def handle = "mainview"
    override def description = "main view state"
    override def nativeBack() = mainView.nativeBack()
  }

  override def impls = mainViewImpl :: super.impls

  implicit val fm: FragmentManagement = mainView

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
    attachRoot(FL(bgCol("main"), metaName("root frame"))(
      l[FrameLayout]() <~ content <~ Id.content <~ metaName("content frame")))
  }

  def loadFragment(fragment: Fragment) = {
    send(LoadUi(frag(fragment, Id.content)))
  }

  def loadShowFragment[A <: SyncModel: ClassTag]
  (model: A, ctor: () ⇒ ShowFragment[A]) {
    send(LoadUi(showFrag(model, ctor, Id.content)))
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
    send(Back)
  }

  def nativeBack() {
    super.onBackPressed()
  }

  def canGoBack = backStackNonEmpty

  lazy val mainActor = createActor(MainActor.props)._2

  def showDetails(data: Model) {}
}
