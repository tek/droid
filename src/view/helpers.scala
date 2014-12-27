package tryp.droid.view

import scala.language.dynamics
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

import scalaz._
import Scalaz.{Id ⇒ sId,_}

import android.view.{View,ViewGroup}
import android.widget.{AdapterView,TextView}
import android.content.res.{Resources,Configuration}
import android.content.{Context,DialogInterface}
import android.view.inputmethod.InputMethodManager
import android.app.{Activity ⇒ AActivity,AlertDialog,DialogFragment,Dialog}
import android.app.{FragmentManager, Fragment ⇒ AFragment}
import android.os.Bundle

import macroid.{FragmentManagerContext,ActivityContext,AppContext,Ui}
import macroid.support.FragmentApi

import tryp.droid.util._
import tryp.droid.{Basic ⇒ BBasic}
import tryp.droid.AndroidExt._

trait ProxyBase {
  def extractView(args: Any*): View = {
    if (args.isEmpty) null else args.head match {
      case v: View ⇒ v
      case _ ⇒ null
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
      case v: View ⇒ Option(v)
      case null ⇒ {
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
      case a: Option[B] ⇒ a
      case a ⇒ {
        Log.e(s"Couldn't cast view ${a} to specified type!")
        None
      }
    }
  }

  def viewsOfType[A <: View: ClassTag]: Seq[A] = {
    view match {
      case layout: ViewGroup ⇒ {
        layout.children map {
          case v: A ⇒ Seq(v)
          case sub: ViewGroup ⇒ sub.viewsOfType[A]
          case _ ⇒ Nil
        } flatten
      }
      case v: A ⇒ Seq(v)
      case _ ⇒ Nil
    }
  }

  def viewOfType[A <: View: ClassTag] = {
    viewsOfType[A].lift(0)
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

  def uiThread(callback: ⇒ Unit) {
    val runner = new Runnable {
      def run = callback
    }
    Option(activity) foreach { _.runOnUiThread(runner) }
  }

  def inflateLayout[A <: View: ClassTag](name: String): Ui[A] = {
    Ui {
      activity.getLayoutInflater.inflate(layoutId(name), null) match {
        case view: A ⇒ view
        case view ⇒ {
          throw new ClassCastException(
            s"Inflated layout ${name} resulted in wrong type " +
            s"'${view.className}'")
        }
      }
    }
  }
}

trait Basic
extends Activity
with Searchable

trait Click
extends Basic
{
  def itemClickListen(view: AdapterView[_], callback: (View) ⇒ Unit) {
    view.setOnItemClickListener(new AdapterView.OnItemClickListener {
      def onItemClick(parent: AdapterView[_], view: View, pos: Int, id: Long) {
        callback(view)
      }
    })
  }

  def clickListen(view: View, callback: (View) ⇒ Unit) {
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
      case a: InputMethodManager ⇒ a
      case _ ⇒ {
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
extends tryp.droid.Basic
{
  lazy val theme = new tryp.droid.view.Theme
}

class DialogListener(callback: () ⇒ Unit = () ⇒ ())
extends DialogInterface.OnClickListener
{
  def onClick(dialog: DialogInterface, id: Int) {
    callback()
  }
}

class ConfirmDialog(message: String, callback: () ⇒ Unit)
extends DialogFragment
with tryp.droid.Basic
{
  def context = getActivity

  override def onCreateDialog(state: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(context)
    builder.setMessage(message)
    builder.setPositiveButton(string("yes"), new DialogListener(callback))
    builder.setNegativeButton(string("no"), new DialogListener)
    builder.create
  }
}

trait Confirm
extends Activity
{
  def confirm(message: String, callback: () ⇒ Unit) {
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
  def getFragmentManager: FragmentManager

  def rootFragmentManager = getFragmentManager

  def fragmentManager = rootFragmentManager

  def findFragment(tag: String) = {
    Option[AFragment](getFragmentManager.findFragmentByTag(tag))
  }

  def replaceFragment[A >: BBasic#IdTypes](
    name: A, fragment: AFragment, backStack: Boolean, tag: String) {
    val trans = fragmentManager.beginTransaction
    trans.replace(id(name), fragment, tag)
    if (backStack) {
      trans.addToBackStack(tag)
    }
    trans.commit
  }

  // TODO allow overriding the check for existence for back stack fragments
  def replaceFragmentIf(name: Id, fragment: AFragment, backStack: Boolean,
    tag: String)
  {
    findFragment(tag) getOrElse {
      replaceFragment(name, fragment, false, tag)
    }
  }

  def replaceFragmentCustom[A <: AFragment: ClassTag](id: Id, fragment: A,
    backStack: Boolean)
  {
    replaceFragmentIf(id, fragment, backStack, fragmentName[A])
  }

  def replaceFragmentAuto[A <: AFragment: ClassTag](id: Id, backStack: Boolean)
  {
    val tag = Tag(fragmentName[A])
    replaceFragment(id, makeFragment[A], backStack, tag)
  }

  def addFragment[A >: BBasic#IdTypes](name: A, fragment: AFragment,
    backStack: Boolean, tag: String)
  {
    val trans = fragmentManager.beginTransaction
    trans.add(id(name), fragment, tag)
    if (backStack) {
      trans.addToBackStack(tag)
    }
    trans.commit
  }

  def addFragmentIf[A <: AFragment: ClassTag](ctor: ⇒ A) {
    val name = fragmentName[A]
    val tag = Tag(name)
    findFragment(tag) getOrElse {
      replaceFragment(Id(name), ctor, false, tag)
    }
  }

  def addFragmentIfAuto[A <: AFragment: ClassTag] {
    addFragmentIf { makeFragment[A] }
  }

  def fragmentClassName(cls: Class[_]) = {
    cls.className.stripSuffix("Fragment")
  }

  def fragmentName[A <: AFragment: ClassTag] = {
    fragmentClassName(implicitly[ClassTag[A]].runtimeClass)
  }

  def makeFragment[A <: AFragment: ClassTag] = {
    val cls = implicitly[ClassTag[A]].runtimeClass
    cls.newInstance.asInstanceOf[AFragment]
  }

  def popBackStackSync {
    rootFragmentManager.popBackStackImmediate
  }

  def findNestedFrag[A <: AFragment: ClassTag](
    tags: String*
  ): Option[AFragment] = {
    tags lift(0) flatMap { findFragment } flatMap { frag ⇒
      frag.findNestedFrag(tags.tail: _*) orElse {
        frag match {
          case f: A ⇒ f.some
          case _ ⇒ None
        }
      }
    }
  }
}
