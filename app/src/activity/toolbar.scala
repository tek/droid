package tryp
package droid

import android.support.v7.app.ActionBarActivity
import android.support.v7.widget.Toolbar
import android.view.Gravity

import macroid.FullDsl._

import ViewExports._
import ScalazGlobals._

trait HasToolbar
extends MainView
{ self: ActionBarActivity
  with Akkativity =>

    import tryp.droid.tweaks.{Toolbar => T}

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
    val t = theme.dimension("actionBarSize")
      .map(a => T.minHeight(a.toInt)).toOption
    l[Toolbar](l[FrameLayout]() <~ RId.toolbar <~ ↔) <~
      whore(toolbar) <~
      bgCol("toolbar") <~
      t <~
      T.titleColor("toolbar_text") <~
      toolbarLp(↔, Height.wrap, Gravity.RIGHT)
  }

  def belowToolbarLayout: Ui[View] = contentLayout

  def toolbarTitle(title: String) = {
    toolbar <~ T.title(
      title.isEmpty ? (res.string("app_title") getOrElse("app")) | title)
  }

  def toolbarView(view: Fragment) {
    // this.replaceFragmentCustom(RId.toolbar, view, false)
  }

  def navButtonClick() = {
    canGoBack tapIf { onBackPressed() }
  }
}
