package tryp.droid.activity

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global

import android.content.SharedPreferences
import android.content.res.Configuration
import android.view.{MenuItem,Gravity}
import android.preference.PreferenceManager
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.{ActionBarDrawerToggle,ActionBarActivity}
import android.support.v7.widget.Toolbar

import com.google.android.gms.common._
import com.google.android.gms.auth._

import android.transitions.everywhere.TransitionSet

import macroid.FullDsl._

import rx._
import rx.ops._

import tryp.droid._
import tryp.droid.util.CallbackMixin
import Macroid._
import Screws._

trait ActivityBase
extends Activity
with HasActivity
{
}

trait Theme extends ActivityBase
{ self: Preferences ⇒

  private var themeInitialized = false

  abstract override def onCreate(state: Bundle) {
    observeTheme
    themeInitialized = true
    super.onCreate(state)
  }

  lazy val currentTheme = prefs.string("theme", defaultTheme)

  lazy val observeTheme = Obs(currentTheme) {
    changeTheme(currentTheme(), themeInitialized)
  }

  def changeTheme(name: String, restart: Boolean)

  def defaultTheme: String
}

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
    setContentView(getUi(mainLayout))
  }

  def mainLayout = contentLayout

  def contentLayout: Ui[ViewGroup] = {
    attachRoot(FL(bgCol("main"))(l[FrameLayout]() <~ Id.content))
  }

  def loadFragment(fragment: Fragment) = {
    loadView(frag(fragment, Id.content))
  }

  def loadShowFragment[A <: Model: ClassTag]
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
    canGoBack ? goBack() / super.onBackPressed()
  }

  def goBack() {
    popBackStackSync
  }

  def canGoBack = backStackNonEmpty

  lazy val mainActor = createActor(MainActor.props)._2

  def showDetails(data: Model) {  }
}

abstract trait ManagePreferences
extends ActivityBase
with OnSharedPreferenceChangeListener
with Preferences
{
  self: MainView
  with FragmentManagement ⇒

  abstract override def onCreate(state: Bundle) {
    setupPreferences
    super.onCreate(state)
  }

  override def onSharedPreferenceChanged(prfs: SharedPreferences, key: String)
  {
    val value = prfs.getAll.get(key)
    val validated = validatePreference(key, value)
    if (validated != value) {
      prefs.edit { editor ⇒
        value match {
          case s: String ⇒ editor.putString(key, s)
          case b: Boolean ⇒ editor.putBoolean(key, b)
          case _ ⇒
        }
      }
    }
    changePref(key, validated)
  }

  protected def validatePreference(key: String, value: Any): Any = {
    value
  }

  abstract override def onResume {
    super.onResume
    prefs.registerListener(this)
  }

  abstract override def onPause {
    super.onPause
    prefs.unregisterListener(this)
  }

  def settings() {
    loadFragment(Classes.fragments.settings())
  }

  def setupPreferences {
    PreferenceManager.setDefaultValues(activity, res.xmlId("user_preferences"),
      false)
  }

  def changePref(key: String, value: Any) {
    prefs.change(key, value)
    key match {
      case "pref_theme" ⇒ value match {
        case s: String ⇒ changeTheme(s)
        case _ ⇒ prefError(key, value)
      }
      case _ ⇒
    }
  }

  protected def prefError(key: String, value: Any) {
    Log.e("Invalid value for preference ${key}: ${value} (${value.getClass})")
  }

  // FIXME cannot resolve theme id in robolectric test
  def changeTheme(theme: String, restart: Boolean = true) {
    val id = res.themeId(theme)
    if (id > 0) {
      applyTheme(id, restart)
    }
  }

  def applyTheme(id: Int, restart: Boolean = true) {
    activity.getApplicationContext.setTheme(id)
    activity.setTheme(id)
    if (restart) activity.recreate
  }

  def popSettings {
    goBack
  }
}

trait System
extends ActivityBase
{
  import android.view.View

  def hideStatusBar {
    activity.getWindow.getDecorView.setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_FULLSCREEN)
  }
}

trait HasToolbar
extends MainView
{ self: ActionBarActivity
  with FragmentManagement
  with Akkativity ⇒

    import tryp.droid.tweaks.{Toolbar ⇒ T}

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    toolbar foreach setSupportActionBar
    runUi(toolbar <~ T.navButtonListener(navButtonClick()))
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
    l[Toolbar](l[FrameLayout]() <~ Id.toolbar) <~
      ↔ <~
      whore(toolbar) <~
      bgCol("toolbar") <~
      T.minHeight(theme.dimension("actionBarSize").toInt) <~
      T.titleColor("toolbar_text")
  }

  def belowToolbarLayout: Ui[View] = contentLayout

  def toolbarTitle(title: String) {
    runUi {
      toolbar <~
        T.title(title.isEmpty ? res.string("app_title") / title)
    }
  }

  def toolbarView(view: Fragment) {
    replaceFragmentCustom(Id.toolbar, view, false)
  }

  def navButtonClick() = {
    canGoBack tapIf { onBackPressed() }
  }
}

trait HasNavigation
extends MainView
{ self: FragmentManagement
  with Akkativity ⇒

  val navigation: Navigation

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    resumeNavigation()
  }

  def resumeNavigation() {
    if (history.isEmpty) navigateIndex(0)
    else history.lastOption foreach(loadNavTarget)
  }

  def navigateIndex(index: Int) {
    navigation.targets.lift(index) foreach(navigate)
  }

  def navigate(target: NavigationTarget) {
    if (!navigation.current.contains(target)) {
      if (!tryPopHome(target)) history = target :: history
      loadNavTarget(target)
    }
  }

  def loadNavTarget(target: NavigationTarget) {
    ui { loadView(target.create(Id.content)) }
    navigation.current = Some(target)
    navigated(target)
  }

  def tryPopHome(target: NavigationTarget) = {
    target.home && {
      clearHistory(target)
      history.headOption contains target
    }
  }

  def clearHistory(target: NavigationTarget) = {
    history = history drop(history indexOf target)
  }

  var history: List[NavigationTarget] = List()

  override def goBack() {
    history = history.tail
    history.headOption foreach(loadNavTarget)
  }

  override def canGoBack = history.length > 1

  def navigated(target: NavigationTarget) {
  }

  def loadFragment(name: String, ctor: () ⇒ Fragment) {
    val target = new NavigationTarget(name, ctor)
    navigate(target)
  }

  override def loadShowFragment[A <: slick.db.Model: ClassTag]
  (model: A, ctor: () ⇒ ShowFragment[A]) {
    val target = new ShowNavigationTarget("Details", ctor, model)
    navigate(target)
  }
}

trait Drawer
extends HasToolbar
with HasNavigation
with DrawerLayout.DrawerListener
{ self: ActionBarActivity
  with FragmentManagement
  with Akkativity ⇒

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
    if (drawerOpen) {
      closeDrawer.run
    }
    else if (!super.navButtonClick()) {
      openDrawer.run
    }
    true
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
}

trait GPlusIntegration
extends ActivityBase
with AppPreferences
{ self: Akkativity ⇒

  abstract override def onCreate(state: Bundle) {
    super.onCreate(state)
    authTokenObserver
  }

  var fetchingPlusToken = false

  def authToken: Var[String]

  def authorizeToken(account: String, plusToken: String)

  lazy val authTokenState = Rx { (authToken(), GPlus.signedIn()) }

  lazy val authTokenObserver = Obs(authTokenState) {
    if (authToken() == "" && !fetchingPlusToken) obtainToken()
  }

  def obtainToken() {
    fetchingPlusToken = true
    fetchPlusToken.future andThen {
      case Success((account, plusToken)) ⇒
        authorizeToken(account, plusToken)
      case Failure(ex: UserRecoverableAuthException) ⇒
        requestPermission(ex.getIntent)
      case Failure(ex) ⇒
        Log.e(s"Google Plus auth token request failed: ${ex}")
    } onComplete { _ ⇒ fetchingPlusToken = false }
  }

  def serverClientId(implicit c: Context) =
    res.string("gplus_oauth_server_client_id")

  val scopes = Scopes.PLUS_LOGIN

  def scopeString(implicit c: Context) =
    s"oauth2:server:client_id:$serverClientId:api_scope:$scopes"

  def fetchPlusToken = {
    GPlus { plus ⇒
      plus.email map {e ⇒ (e, plusToken(e)) } getOrElse {
        throw new Exception("Plus account name unavailable")
      }
    }
  }

  def plusToken(email: String) =
    GoogleAuthUtil.getToken(this, email, scopeString)

  def requestPermission(intent: Intent) =
    startActivityForResult(intent, GPlusBase.RC_TOKEN_FAIL)

  def plusTokenResolved(success: Boolean) {
    if (success) obtainToken()
  }

  override def onActivityResult(requestCode: Int, responseCode: Int, intent:
    Intent)
  {
    if (requestCode == GPlusBase.RC_SIGN_IN)
      GPlus.signInComplete(responseCode == android.app.Activity.RESULT_OK)
    else if (requestCode == GPlusBase.RC_TOKEN_FAIL)
      plusTokenResolved(responseCode == android.app.Activity.RESULT_OK)
  }
}
