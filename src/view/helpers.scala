package tryp.droid.view

import scala.language.dynamics
import scala.reflect.ClassTag

import android.view.View
import android.widget.{AdapterView,TextView}
import android.content.res.{Resources,Configuration}
import android.content.{Context,DialogInterface}
import android.view.inputmethod.InputMethodManager
import android.app.{Activity => AActivity,AlertDialog,DialogFragment,Dialog}
import android.os.Bundle

import macroid.{FragmentManagerContext,ActivityContext,AppContext}
import macroid.support.FragmentApi

import tryp.droid.util.Id
import tryp.droid.{Basic ⇒ BBasic}

trait ProxyBase {
  def extractView(args: Any*): View = {
    if (args.isEmpty) null else args.head match {
      case v: View => v
      case _ => null
    }
  }
}

case class ViewsProxy(owner: Searchable)
extends Dynamic
with ProxyBase
{
  def applyDynamic(name: String)(args: Any*): Option[View] = {
    owner.find(name, extractView(args))
  }

  def selectDynamic(name: String): Option[View] = {
    owner.find(name)
  }
}

case class TypedViewsProxy(owner: Searchable)
extends Dynamic
with ProxyBase
{
  def applyDynamic[A <: View: ClassTag](name: String)(args: Any*): Option[A] = {
    owner.findt[String, A](name, extractView(args))
  }

  def selectDynamic[A <: View: ClassTag](name: String): Option[A] = {
    owner.findt[String, A](name)
  }
}

trait Searchable {
  def view: View

  def id[A >: BBasic#IdTypes](input: A, defType: String = "id"): Int

  def find[A >: BBasic#IdTypes](name: A, root: View = null): Option[View] = {
    val entry = if (root != null) root else view
    entry.findViewById(id(name)) match {
      case v: View => Option(v)
      case null => {
        if (Env.release) {
          None
        }
        else {
          val msg = s"Couldn't find a view with id '${name}'! " +
          s"Current views: ${Id.ids}"
          throw new ClassCastException(msg)
        }
      }
    }
  }

  def findt[A >: BBasic#IdTypes, B <: View: ClassTag](
    name: A, root: View = null
  ): Option[B] = {
    find(name, root) match {
      case a: Option[B] => a
      case a => {
        Log.e(s"Couldn't cast view ${a} to specified type!")
        None
      }
    }
  }

  def textView[A >: BBasic#IdTypes](
    name: A, root: View = null
  ): Option[TextView] = {
    findt[A, TextView](name, root)
  }

  lazy val tviews = TypedViewsProxy(this)

  lazy val views = ViewsProxy(this)
}

trait ActivityContexts {

  implicit def activityAppContext(implicit activity: AActivity) =
    AppContext(activity.getApplicationContext)

  implicit def activityManagerContext[M, F](implicit fragmentApi:
    FragmentApi[F, M, AActivity], activity: AActivity) =
      FragmentManagerContext[F, M](fragmentApi.activityManager(activity))

  implicit def activityActivityContext(implicit activity: AActivity) =
    ActivityContext(activity)
}

trait Activity
extends tryp.droid.Basic
{
  implicit def activity: AActivity

  implicit def context: Context = activity

  def uiThread(callback: => Unit) {
    val runner = new Runnable {
      def run = callback
    }
    activity.runOnUiThread(runner)
  }
}

trait Basic
extends Activity
with Searchable

trait Click
extends Basic
{
  def itemClickListen(view: AdapterView[_], callback: (View) => Unit) {
    view.setOnItemClickListener(new AdapterView.OnItemClickListener {
      def onItemClick(parent: AdapterView[_], view: View, pos: Int, id: Long) {
        callback(view)
      }
    })
  }

  def clickListen(view: View, callback: (View) => Unit) {
    view.setOnClickListener(new View.OnClickListener {
      def onClick(view: View) = callback(view)
    })
  }
}

trait Geometry
extends Basic
{
  def resources: Resources

  def viewPos(view: View): Array[Int] = {
    val data = Array[Int](0, 0)
    view.getLocationOnScreen(data)
    data
  }

  def isPortraitMode {
    activity.getResources.getConfiguration.orientation ==
      Configuration.ORIENTATION_PORTRAIT
  }
}

trait Input
extends Basic
{
  def inputMethodManager: InputMethodManager = {
    activity.getSystemService(Context.INPUT_METHOD_SERVICE) match {
      case a: InputMethodManager => a
      case _ => {
        throw new ClassCastException(
          "Wrong class for InputMethodManager!"
        )
      }
    }
  }

  def hideKeyboard {
    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken, 0)
  }
}

trait Themes
extends Activity
{
  lazy val theme = new tryp.droid.view.Theme
}

class DialogListener(callback: () => Unit = () => ())
extends DialogInterface.OnClickListener
{
  def onClick(dialog: DialogInterface, id: Int) {
    callback()
  }
}

class ConfirmDialog(message: String, callback: () => Unit)
extends DialogFragment
with FragmentBase
{
  override def onCreate(state: Bundle) = super.onCreate(state)
  override def onStart = super.onStart
  override def onStop = super.onStop
  override def onViewStateRestored(state: Bundle) = {
    super.onViewStateRestored(state)
  }
  override def onActivityCreated(state: Bundle) {
    super.onActivityCreated(state)
  }

  override def onCreateDialog(state: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(activity)
    builder.setMessage(message)
    builder.setPositiveButton(string("yes"), new DialogListener(callback))
    builder.setNegativeButton(string("no"), new DialogListener)
    builder.create
  }
}

trait Confirm
extends Activity
{
  def confirm(message: String, callback: () => Unit) {
    val dialog = new ConfirmDialog(message, callback)
    dialog.show(activity.getFragmentManager, "confirm")
  }
}

trait Preferences
extends Activity
with tryp.droid.Preferences
{
  def appPrefs = activity.getPreferences(Context.MODE_PRIVATE)

  def appPref(name: String, default: String = "") = {
    appPrefs.getString(name, default)
  }

  def appPrefBool(name: String, default: Boolean = true) = {
    appPrefs.getBoolean(name, default)
  }

  def setAppPref(name: String, value: String) {
    editPrefs(_.putString(name, value), appPrefs)
  }

  def setAppPrefBool(name: String, value: Boolean) {
    editPrefs(_.putBoolean(name, value), appPrefs)
  }
}

trait Fragments
extends Activity
{
  def replaceFragment[A >: BBasic#IdTypes](
    name: A, fragment: android.app.Fragment, backStack: Boolean
  ) {
    val trans = activity.getFragmentManager.beginTransaction
    trans.replace(id(name), fragment)
    if (backStack) {
      trans.addToBackStack(null)
    }
    trans.commit
  }
}
