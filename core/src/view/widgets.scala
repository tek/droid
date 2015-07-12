package tryp.droid

import scala.concurrent.ExecutionContext.Implicits.global

import android.widget._

import com.melnykov.fab.FloatingActionButton

import macroid.FullDsl._
import macroid.Snails

import tryp.droid.Macroid._
import tryp.droid.{Macroid ⇒ T}

trait Fab
extends Transitions
{ self: MainFragment ⇒

  import CommonWidgets._

  val progress = slut[ProgressBar]

  // Create a wrapper layout containing:
  // * a floating action button, showing 'icon' and dispatching touch to
  //   'onClick'
  // * the View created by the second block arg.
  def fabCorner(icon: String)(contentView: ⇒ Ui[View]) =
  {
    val geom = rlp(↧, ↦) + margin(right = 16 dp, bottom = 48 dp)
    RL(rlp(↔, ↕))(
      content(contentView),
      progressUi <~ geom,
      fabUi(icon) <~ geom
    )
  }

  def fabBetween(icon: String, parallax: Boolean = false)
  (headerView: Ui[View], contentView: Ui[View]) =
  {
    val geom = rlp(↦, alignBottom(Id.header)) +
      margin(right = 16 dp,
        bottom = res.dimen("fab_margin_normal_minus").toInt)
    val contentParams = rlp(parallax ? ↥ | below(Id.header))
    RL(rlp(↔, ↕))(
      content(contentView) <~ contentParams,
      header(RL(rlp(↔, ↕))(headerView) <~ bgCol("header")) <~ Id.header <~
        rlp(↥, ↔, Height(headerHeight)),
      progressUi <~ geom,
      fabUi(icon) <~ geom
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

  def fabClick() {  }

  // Runs 'task' in a future while changing the fab to a circular progress
  // indicator. After completion, 'snack' is shown as a toast, if nonempty.
  def fabAsync[A, B](snack: Option[String] = None)(task: ⇒ B)
  (callback: (B) ⇒ Unit) = {
    Future { task }
  }

  def fabAsyncF[A, B](snack: ⇒ Option[String] = None)(task: Future[B])
  (callback: (B) ⇒ Unit) = {
    task mapUi { b ⇒
      callback(b)
      snack map { mkToast(_) } getOrElse Ui.nop
    }
    ((fadeToProgress <~~ Snails.wait(task)) ~~ fadeToFab).run
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
    runUi((fab <~~ snail) ~~ Ui {
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
    runUi(fab <~ translateY(-scrollHeight))
    syncFabVisibility()
  }

  def fabHideThresh = headerHeight / 2

  def scrolled(view: ViewGroup, height: Int) {
    scrollHeight = height
    updateFabPosition()
    runUi(header <~ parallaxScroll(height))
  }
}
