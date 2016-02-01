package tryp
package droid

import macroid.FullDsl._

import MainViewMessages._

import cats._
import cats.syntax.apply._

trait MainView
extends ActivityBase
with Transitions
with ActivityAgent
{
  mainView: Akkativity ⇒

  val content = slut[FrameLayout]

  lazy val mainViewMachine = new MainViewMachine {
    def handle = "mainview"
    override def description = "main view state"
    override def nativeBack() = mainView.nativeBack()
  }

  override def machines = mainViewMachine :: super.machines

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
    val tw = List(bgCol("main"), Some(metaName("root frame"))).flatten
    attachRoot(FL(tw: _*)(
      l[FrameLayout]() <~ content <~ RId.content <~ metaName("content frame")))
  }

  def loadFragment(fragment: Fragment) = {
    val f = frag(this, fragment, RId.content)
    send(LoadUi(f))
  }

  def loadShowFragment[A <: SyncModel: ClassTag]
  (model: A, ctor: () ⇒ ShowFragment[A]) {
    send(LoadUi(showFrag(this, model, ctor, RId.content)))
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

  def canGoBack = this.backStackNonEmpty

  lazy val mainActor = createActor(MainActor.props)._2

  def showDetails(data: Model) {}
}
