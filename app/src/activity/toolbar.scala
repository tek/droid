package tryp
package droid

import android.support.v7.app.ActionBarActivity
import android.support.v7.widget.Toolbar
import android.view.Gravity

import macroid.FullDsl._

trait HasToolbar
extends MainView
{ self: ActionBarActivity
  with FragmentManagement
  with Akkativity ⇒

    import tryp.droid.tweaks.{Toolbar ⇒ T}

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    toolbar foreach setSupportActionBar
    Ui.run(toolbar <~ T.navButtonListener(navButtonClick()))
  }

  val toolbar = slut[Toolbar]

  override def mainLayout = {
    l[FrameLayout](
      LL(vertical, llp(↔, ↕))(
        toolbarLayout,
        belowToolbarLayout
      )
    ) <~ fitsSystemWindows
  }

  def toolbarLayout = {
    l[Toolbar](l[FrameLayout]() <~ Id.toolbar <~ ↔) <~
      whore(toolbar) <~
      bgCol("toolbar") <~
      T.minHeight(theme.dimension("actionBarSize").toInt) <~
      T.titleColor("toolbar_text") <~
      toolbarLp(↔, Height.wrap, Gravity.RIGHT)
  }

  def belowToolbarLayout: Ui[View] = contentLayout

  def toolbarTitle(title: String) = {
    toolbar <~ T.title(title.isEmpty ? res.string("app_title") | title)
  }

  def toolbarView(view: Fragment) {
    replaceFragmentCustom(Id.toolbar, view, false)
  }

  def navButtonClick() = {
    canGoBack tapIf { onBackPressed() }
  }
}
