package tryp.droid

import android.support.v4.widget.DrawerLayout
import android.support.v7.app.{ActionBarDrawerToggle,ActionBarActivity}
import android.content.res.Configuration
import android.view.Gravity

import macroid.FullDsl._

trait Drawer
extends HasToolbar
with HasNavigation
with DrawerLayout.DrawerListener
{ self: ActionBarActivity
  with FragmentManagement
  with Akkativity
  with Stateful ⇒

  import tryp.droid.tweaks.{Toolbar ⇒ T, Drawer ⇒ D}

  override def initView {
    super.initView
    initDrawer
  }

  private lazy val state = actorSystem.actorOf(DrawerState.props)

  def initDrawer {
    replaceFragment(Id.Drawer, Fragments.drawer(), false, Tag.Drawer)
    drawerToggle
    drawerActor ! Messages.Inject("navigation", navigation)
  }

  val drawer = slut[DrawerLayout]

  lazy val drawerToggle = {
    drawer flatMap { drawer ⇒
      toolbar map { tb ⇒
        new ActionBarDrawerToggle(
          activity,
          drawer,
          tb,
          res.stringId("drawer_open"),
          res.stringId("drawer_close")
        )
      }
    }
  }

  override def belowToolbarLayout = {
    l[DrawerLayout](
      contentLayout,
      l[FrameLayout]() <~ Id.Drawer <~ dlp(res.dimen("drawer_width"), ↕)
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

  override def navigate(target: NavigationTarget) {
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

  def drawerClick(action: ViewState.Message) = {
    closeIfOpen()
    send(action)
  }
}
