package tryp
package droid

import android.support.v4.widget.DrawerLayout
import android.support.v7.app.{ActionBarDrawerToggle,ActionBarActivity}
import android.content.res.Configuration
import android.view.Gravity

import macroid.FullDsl._

import cats.syntax.apply._

trait DrawerBase
extends HasToolbar
with HasNavigation
with DrawerLayout.DrawerListener
{ self: ActionBarActivity
  with Akkativity
  with HasContextAgent ⇒

  import tryp.droid.tweaks.{Toolbar ⇒ T, Drawer ⇒ D}

  override def initView {
    super.initView
    initDrawer()
  }

  private lazy val state = self.actorSystem.actorOf(DrawerState.props)

  def initDrawer() {
    this.replaceFragment(RId.Drawer, Fragments.drawer(), false, Tag.Drawer)
    drawerToggle
    drawerActor ! Messages.Inject("navigation", navigation)
  }

  val drawer = slut[DrawerLayout]

  lazy val drawerToggle = {
    drawer flatMap { drawer ⇒
      toolbar flatMap { tb ⇒
        res.stringId("drawer_open") |@| res.stringId("drawer_close") map {
          case (o, c) ⇒
            new ActionBarDrawerToggle(activity, drawer, tb, o, c)
        } toOption
      }
    }
  }

  override def belowToolbarLayout = {
    val w: Float = res.dimen("drawer_width") getOrElse 500.dp
    l[DrawerLayout](
      contentLayout,
      l[FrameLayout]() <~ RId.Drawer <~ dlp(w, ↕)
    ) <~ whore(drawer) <~ llp(↔, ↕) <~ D.listener(this)
  }

  abstract override def onPostCreate(state: Bundle) {
    super.onPostCreate(state)
    syncToggle()
  }

  def syncToggle() {
    drawerToggle <<~ D.sync
  }

  abstract override def onConfigurationChanged(newConf: Configuration) {
    super.onConfigurationChanged(newConf)
    drawerToggle foreach { _.onConfigurationChanged(newConf) }
  }

  def drawerOpen = drawer exists { _.isDrawerOpen(Gravity.LEFT) }

  def closeDrawer = drawer <~ D.close()

  def openDrawer = drawer <~ D.open()

  override def contentLoaded() {
    updateToggle()
  }

  def updateToggle() {
    if (canGoBack) enableBackButton()
    else syncToggle()
  }

  def enableBackButton() {
    drawerToggle foreach { _.onDrawerOpened(null) }
  }

  def onDrawerOpened(drawerView: View) {
    drawerToggle foreach { _.onDrawerOpened(drawerView) }
    state ! DrawerOpened
  }

  def onDrawerClosed(drawerView: View) {
    drawerToggle foreach { _.onDrawerClosed(drawerView) }
    state ! DrawerClosed(updateToggle)
  }

  def onDrawerSlide(drawerView: View, slideOffset: Float) {
    drawerToggle foreach { _.onDrawerSlide(drawerView, slideOffset) }
  }

  def onDrawerStateChanged(newState: Int) {
    drawerToggle foreach { _.onDrawerStateChanged(newState) }
  }

  override def navButtonClick() = {
    if (drawerOpen) closeDrawer.run
    else if (!super.navButtonClick()) openDrawer.run
    true
  }

  def closeIfOpen() = {
    if (drawerOpen) closeDrawer.run
  }

  override def navigate(target: NavigationTarget) = {
    if (drawerOpen) {
      state ! DrawerNavigated { () ⇒ super.navigate(target) }
      closeDrawer.run
    }
    else super.navigate(target)
  }

  lazy val drawerActor = createActor(DrawerActor.props)._2

  override def navigated(target: NavigationTarget) {
    drawerActor ! Messages.Navigation(target)
  }

  def drawerClick(action: droid.state.Message) = {
    closeIfOpen()
    send(action)
  }
}

trait Drawer
extends DrawerBase
{ self: ActionBarActivity
  with Akkativity
  with HasContextAgent ⇒

}
