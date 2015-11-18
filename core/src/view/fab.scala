package tryp
package droid

import concurrent._
import duration.Duration

import scalaz._, Scalaz._, concurrent._

import android.widget._

import com.melnykov.fab.FloatingActionButton

import macroid._
import FullDsl._

import AsyncTaskStateData._

trait Fab
extends StatefulHasActivity
with Transitions
with Macroid
{ fabView ⇒

  import CommonWidgets._

  lazy val asyncImpl = new AsyncTasksImpl {
    def handle = "fab"
    override def switchToAsyncUi = fabView.fadeToProgress
    override def switchToIdleUi = fabView.fadeToFab
  }

  override def impls = asyncImpl :: super.impls

  val progress = slut[ProgressBar]

  def fabCorner(icon: String)(contentView: ⇒ Ui[View]) = {
    val geom = rlp(↧, ↦) + margin(right = 16 dp, bottom = 48 dp)
    RL(rlp(↔, ↕), metaName("fab corner container"))(
      content(contentView) <~ metaName("content view"),
      progressUi <~ geom <~ metaName("progress indicator"),
      fabUi(icon) <~ geom <~ metaName("fab")
    )
  }

  def fabBetween(icon: String, parallax: Boolean = false)
  (headerView: Ui[View], contentView: Ui[View]) =
  {
    val geom = rlp(↦, alignBottom(Id.header)) +
      margin(right = 16 dp,
        bottom = res.dimen("fab_margin_normal_minus").toInt)
    val contentParams = rlp(parallax ? ↥ | below(Id.header))
    RL(rlp(↔, ↕), metaName("fab corner container"))(
      content(contentView) <~ contentParams <~ metaName("content view"),
      header(
        RL(rlp(↔, ↕))(
          headerView <~ metaName("fab header")
        ) <~ bgCol("header") <~ metaName("fab header container")
      ) <~ Id.header <~ rlp(↥, ↔, Height(headerHeight)),
      progressUi <~ geom <~ metaName("progress indicator"),
      fabUi(icon) <~ geom <~ metaName("fab")
    )
  }

  def progressUi = w[ProgressBar] <~ indeterminate <~ hide <~
        Width(res.dimen("fab_width").toInt) <~ whore(progress)

  def fabUi(icon: String) = {
    fab() <~
      image(icon) <~
      imageScale(ImageView.ScaleType.CENTER) <~
      Fab.colors("colorAccentStrong", "colorAccent") <~
      On.click { Ui(fabClick()) }
  }

  def fabClick() { }

  // Runs 'task' while changing the fab to a circular progress indicator. After
  // completion, 'snack' is shown as a toast, if nonempty.
  // TODO queue into Process
  def fabAsyncF[A, B]
  (success: ⇒ Option[String] = None, failure: ⇒ Option[String] = None)
  (f: Future[B]) = {
    fabAsync(success, failure)(f.task)
  }

  def fabAsync[A, B]
  (success: ⇒ Option[String] = None, failure: ⇒ Option[String] = None)
  (task: Task[B]) = {
    send(AsyncTask(task, success, failure))
  }

  private val fadeTime = 400L

  lazy val fadeToProgress = (fab <~~ fadeOut(fadeTime) <~ hide) ~
    (progress <~~ fadeIn(fadeTime) <~ show)

  lazy val fadeToFab = (progress <~~ fadeOut(fadeTime) <~ hide) ~
    (fab <~~ fadeIn(fadeTime) <~ show)

  lazy val headerHeight = res.dimen("header_height")

  val lock = new Object

  var changingFabVisibility = false

  var scrollHeight = 0

  def changeFabVisibility(snail: Snail[View]) {
    changingFabVisibility = true
    Ui.run((fab <~~ snail) ~~ Ui {
      changingFabVisibility = false
      syncFabVisibility()
    })
  }

  def fabVisible = fab.ui.exists(_.isShown)

  def showFab() {
    if(!fabVisible) changeFabVisibility(fadeIn(fadeTime))
  }

  def hideFab() {
    if(fabVisible) changeFabVisibility(fadeOut(fadeTime))
  }

  def syncFabVisibility() {
    lock synchronized {
      if (!changingFabVisibility)
        if(scrollHeight < fabHideThresh) showFab() else hideFab()
    }
  }

  def updateFabPosition() {
    Ui.run(fab <~ translateY(-scrollHeight))
    syncFabVisibility()
  }

  def fabHideThresh = headerHeight / 2

  def scrolled(view: ViewGroup, height: Int) {
    scrollHeight = height
    updateFabPosition()
    Ui.run(header <~ parallaxScroll(height))
  }
}
