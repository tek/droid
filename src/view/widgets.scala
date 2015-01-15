package tryp.droid

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import android.widget._

import com.melnykov.fab.FloatingActionButton

import macroid.FullDsl._
import macroid.Snails

import tryp.droid.Macroid._
import tryp.droid.{Macroid ⇒ T}

trait Fab
{ self: HasActivity
  with Snackbars ⇒

  val faButton = slut[FloatingActionButton]

  val progress = slut[ProgressBar]

  // Create a wrapper layout containing:
  // * a floating action button, showing 'icon' and dispatching touch to
  //   'onClick'
  // * the View created by the second block arg.
  def fabCorner(icon: String)(onClick: ⇒ Unit)(content: ⇒ Ui[View]) =
  {
    val geom = rlp(↧, ↦) + margin(right = 16 dp, bottom = 48 dp)
    RL()(
      content,
      progressUi <~ geom,
      fabUi(icon)(onClick) <~ geom
    )
  }

  def fabBetween(icon: String, headerId: Id)(onClick: ⇒ Unit)(header: Ui[View],
    content: Ui[View]) =
  {
    val geom = rlp(↦, alignBottom(headerId)) +
      margin(right = 16 dp,
        bottom = res.dimen("fab_margin_normal_minus").toInt)
    RL()(
      header <~ headerId,
      content <~ rlp(below(headerId)),
      progressUi <~ geom,
      fabUi(icon)(onClick) <~ geom
    )
  }

  def progressUi = w[ProgressBar] <~ indeterminate <~ hide <~
        Width(res.dimen("fab_width").toInt) <~ whore(progress)

  def fabUi(icon: String)(onClick: ⇒ Unit) = {
    w[FloatingActionButton] <~
      whore(faButton) <~
      image(icon) <~
      imageScale(ImageView.ScaleType.CENTER) <~
      T.Fab.colors("colorAccentStrong", "colorAccent") <~
      On.click { Ui(onClick) }
  }

  def fabAsync[A, B](snack: Option[String] = None)(task: ⇒ B)
  (callback: (B) ⇒ Unit) = {
    val f = Future { task } mapUi { callback }
    val t = snack map { mkToast(_) }
    ((fadeToProgress <~~ Snails.wait(f)) ~~ fadeToFab ~~ t).run
  }

  private val fadeTime = 400L

  lazy val fadeToProgress = (faButton <~~ fadeOut(fadeTime) <~ hide) ~
    (progress <~~ fadeIn(fadeTime) <~ show)

  lazy val fadeToFab = (progress <~~ fadeOut(fadeTime) <~ hide) ~
    (faButton <~~ fadeIn(fadeTime) <~ show)
}
