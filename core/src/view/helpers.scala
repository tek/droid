package tryp.droid

import scala.language.dynamics
import scala.collection.convert.wrapAll._
import scala.concurrent.duration._
import scala.concurrent.Await

import scalaz._
import Scalaz.{Id ⇒ sId,_}

import android.widget.{AdapterView,TextView}
import android.content.res.{Resources ⇒ AResources,Configuration}
import android.content.DialogInterface
import android.view.inputmethod.InputMethodManager
import android.app.{AlertDialog,DialogFragment,Dialog,PendingIntent}
import android.app.{FragmentManager,FragmentTransaction,AlarmManager}
import android.support.v4.content.WakefulBroadcastReceiver
import android.os.{Vibrator,SystemClock}

import macroid._
import macroid.support.FragmentApi

import util._

trait Searchable
extends Basic
{
  trait ProxyBase {
    def extractView(args: Any*): Option[View] = {
      args.lift(0) flatMap {
        _ match {
          case v: View ⇒ Some(v)
          case _ ⇒ None
        }
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
    def applyDynamic[A <: View: ClassTag](name: String)
    (args: Any*): Option[A] = {
      owner.findt[String, A](name, extractView(args))
    }

    def selectDynamic[A <: View: ClassTag](name: String): Option[A] = {
      owner.findt[String, A](name)
    }
  }

  type CanFindView = AnyRef { def findViewById(id: Int): View }

  def view: View

  def searcher: CanFindView = view

  def find[A >: Basic#IdTypes](
    name: A, root: Option[View] = None
  ): Option[View] = {
    val entry = root getOrElse view
    entry.findViewById(res.id(name)) match {
      case v: View ⇒ Option(v)
      case null ⇒ {
        if (TrypEnv.release) None
        else {
          val msg = s"Couldn't find a view with id '$name'! " +
          s"Current views: ${Id.ids}\n" +
          s"tree:\n" + viewTree.drawTree
          throw new ClassCastException(msg)
        }
      }
    }
  }

  def findt[A >: Basic#IdTypes, B <: View: ClassTag](
    name: A, root: Option[View] = None
  ): Option[B] = {
    find(name, root) match {
      case Some(view: B) ⇒ Some(view)
      case a ⇒ {
        Log.e(s"Couldn't cast view ${a} to specified type!")
        None
      }
    }
  }

  def viewsOfType[A <: View: ClassTag]: Seq[A] = {
    view match {
      case v: A ⇒ Seq(v)
      case layout: ViewGroup ⇒ {
        layout.children map {
          case v: A ⇒ Seq(v)
          case sub: ViewGroup ⇒ sub.viewsOfType[A]
          case _ ⇒ Nil
        } flatten
      }
      case _ ⇒ Nil
    }
  }

  def viewOfType[A <: View: ClassTag] = {
    viewsOfType[A].headOption
  }

  def textView[A >: Basic#IdTypes](
    name: A, root: Option[View] = None
  ): Option[TextView] = {
    findt[A, TextView](name, root)
  }

  def viewExists[A >: Basic#IdTypes](name: A) = {
    Try(find(name)) isSuccess
  }

  def viewTree: Tree[View] = {
    view match {
      case vg: ViewGroup ⇒
        (vg: View).node(vg.children map(_.viewTree): _*)
      case v ⇒
        v.leaf
    }
  }

  lazy val tviews = TypedViewsProxy(this)

  lazy val views = ViewsProxy(this)
}

trait ActivityContexts {
  implicit def toActivityContextWrapper(implicit activity: Activity) =
    ContextWrapper(activity)
}

trait HasActivity
extends Basic
with ActivityContexts
with HasComm
{
  implicit def activity: Activity

  implicit def context: Context = activity

  def uiThread(callback: ⇒ Unit) {
    val runner = new Runnable {
      def run = callback
    }
    Option(activity) foreach { _.runOnUiThread(runner) }
  }

  def ui(callback: ⇒ Unit) = {
    Ui(callback).run
  }

  def uiBlock(callback: ⇒ Unit) {
    Await.ready(ui(callback), Duration.Inf)
  }

  def inflateLayout[A <: View: ClassTag](name: String): Ui[A] = {
    Ui {
      activity.getLayoutInflater.inflate(res.layoutId(name), null) match {
        case view: A ⇒ view
        case view ⇒ {
          throw new ClassCastException(
            s"Inflated layout ${name} resulted in wrong type " +
            s"'${view.className}'")
        }
      }
    }
  }

  implicit lazy val comm = {
    activity match {
      case a: Akkativity ⇒ AkkativityCommunicator(a)
      case _ ⇒ DummyCommunicator()
    }
  }
}

trait ViewBasic
extends HasActivity
with Searchable

trait Click
extends ViewBasic
{
  def itemClickListen(view: AdapterView[_], callback: (View) ⇒ Unit) {
    view.setOnItemClickListener(new AdapterView.OnItemClickListener {
      def onItemClick(parent: AdapterView[_], view: View, pos: Int, id: Long) {
        callback(view)
      }
    })
  }

  def clickListen(view: View, callback: (View) ⇒ Unit) {
    view.setOnClickListener(new android.view.View.OnClickListener {
      def onClick(view: View) = callback(view)
    })
  }
}

trait Geometry
extends ViewBasic
{
  def resources: AResources

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
extends ViewBasic
{
  import android.content.Context

  def inputMethodManager =
    systemService[InputMethodManager](Context.INPUT_METHOD_SERVICE)

  def hideKeyboard {
    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken, 0)
  }
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
with Basic
{
  override implicit def context: Context = getActivity

  override def onCreateDialog(state: Bundle): Dialog = {
    val builder = new AlertDialog.Builder(context)
    builder.setMessage(message)
    builder.setPositiveButton(res.string("yes"), new DialogListener(callback))
    builder.setNegativeButton(res.string("no"), new DialogListener)
    builder.create
  }
}

trait Confirm
extends HasActivity
{
  def confirm(message: String, callback: () ⇒ Unit) {
    val dialog = new ConfirmDialog(message, callback)
    dialog.show(activity.getFragmentManager, "confirm")
  }
}

trait FragmentManagement
extends HasActivity
with Searchable
{
  def getFragmentManager: FragmentManager

  def rootFragmentManager = getFragmentManager

  def fragmentManager = rootFragmentManager

  def findFragment(tag: String) = {
    Option[Fragment](rootFragmentManager.findFragmentByTag(tag))
  }

  def replaceFragment[A >: Basic#IdTypes](name: A, fragment: Fragment,
    backStack: Boolean, tag: String, check: Boolean = true)
  {
    moveFragment(name, fragment, backStack, tag, check) {
      _.replace(res.id(name), fragment, tag)
    }
  }

  // Check for existence of 'fragment' by 'tag', insert the new one if not
  // found
  // Return true if the fragment has been inserted
  // TODO allow overriding the check for existence for back stack fragments
  def replaceFragmentIf(name: Id, fragment: ⇒ Fragment, backStack: Boolean,
    tag: String) =
  {
    val frag = findFragment(tag)
    frag ifNone { replaceFragment(name, fragment, backStack, tag) }
    frag isEmpty
  }

  def replaceFragmentCustom
  (id: Id, fragment: Fragment, backStack: Boolean) =
  {
    replaceFragmentIf(id, fragment, backStack,
      fragmentClassName(fragment.getClass))
  }

  def replaceFragmentAuto[A <: Fragment: ClassTag](id: Id,
    backStack: Boolean) =
  {
    val tag = Tag(fragmentName[A])
    replaceFragmentIf(id, makeFragment[A], backStack, tag)
  }

  def clearBackStack() = {
    backStackNonEmpty tapIf {
        fragmentManager.popBackStack(null,
          FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }
  }

  def addFragment[A >: Basic#IdTypes](name: A, fragment: Fragment,
    backStack: Boolean, tag: String, check: Boolean = true)
  {
    moveFragment(name, fragment, backStack, tag, check) {
      _.add(res.id(name), fragment, tag)
    }
  }

  def moveFragment[A >: Basic#IdTypes](name: A, fragment: Fragment,
    backStack: Boolean, tag: String, check: Boolean = true)
  (move: FragmentTransaction ⇒ Unit)
  {
    checkFrame(name, check) {
      val trans = fragmentManager.beginTransaction
      move(trans)
      if (backStack) {
        trans.addToBackStack(tag)
      }
      trans.commit
    }
  }

  def addFragmentUnchecked[A <: Fragment: ClassTag](ctor: ⇒ A) {
    val name = fragmentName[A]
    addFragment(Id(name), ctor, false, Tag(name), false)
  }

  def addFragmentIf[A <: Fragment: ClassTag](ctor: ⇒ A) {
    val name = fragmentName[A]
    if (!fragmentExists[A]) replaceFragment(Id(name), ctor, false, Tag(name))
  }

  def fragmentExists[A <: Fragment: ClassTag] = {
    val name = fragmentName[A]
    val tag = Tag(name)
    findFragment(tag) isDefined
  }

  def addFragmentIfAuto[A <: Fragment: ClassTag] {
    addFragmentIf { makeFragment[A] }
  }

  def fragmentClassName(cls: Class[_]) = {
    cls.className.stripSuffix("Fragment")
  }

  import scala.reflect.classTag

  def fragmentName[A <: Fragment: ClassTag] = {
    fragmentClassName(classTag[A].runtimeClass)
  }

  def makeFragment[A <: Fragment: ClassTag] = {
    val cls = classTag[A].runtimeClass
    cls.newInstance.asInstanceOf[Fragment]
  }

  def popBackStackSync {
    rootFragmentManager.popBackStackImmediate
  }

  def findNestedFrag[A <: Fragment: ClassTag](
    tags: String*
  ): Option[A] = {
    tags lift(0) flatMap { findFragment } flatMap { frag ⇒
      frag.findNestedFrag(tags.tail: _*) orElse {
        frag match {
          case f: A ⇒ Some(f)
          case _ ⇒ None
        }
      }
    }
  }

  def checkFrame[A >: Basic#IdTypes](name: A, check: Boolean = true)
  (f: ⇒ Unit) {
    if (!check || viewExists(name))
      f
    else
      Log.e(s"Tried to add fragment to nonexistent frame with id '${name}'")
  }

  def backStackEmpty = fragmentManager.getBackStackEntryCount == 0

  def backStackNonEmpty = !backStackEmpty
}

trait Snackbars
extends HasActivity
{
  import tryp.droid.Macroid._
  import macroid.FullDsl.{toast ⇒ mToast,_}

  private implicit val ns = PrefixResourceNamespace("snackbar")

  def snackbar(resName: String) = {
    snackbarImpl(txt.content(resName))
  }

  def snackbarLiteral(message: String) = {
    snackbarImpl(txt.literal(message))
  }

  def snackbarImpl(message: Tweak[TextView]) = {
    val v = LL()(w[TextView] <~ message)
    Ui.run {
      mToast(v) <~ fry
    }
  }

  def toast(resName: String) {
    mkToast(resName).run
  }

  def mkToast(resName: String) = mToast(res.s(resName)) <~ fry
}

trait Alarm
extends Basic
{
  import android.content.Context

  def alarmManager = systemService[AlarmManager](Context.ALARM_SERVICE)

  def scheduleRepeatingWakeAlarm[A <: WakefulBroadcastReceiver: ClassTag]
  (interval: Duration, name: String) =
  {
    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + 10.seconds.toMillis, interval.toMillis,
      alarmIntent[A](name))
  }

  def cancelAlarm[A <: WakefulBroadcastReceiver: ClassTag](name: String) {
    alarmManager.cancel(alarmIntent[A](name))
  }

  def alarmIntent[A <: WakefulBroadcastReceiver: ClassTag](name: String) =
  {
    val intent = new Intent(context, implicitly[ClassTag[A]].runtimeClass)
    intent.putExtra(Keys.intentSource, Values.wakeAlarm)
    intent.putExtra(Keys.alarmPurpose, name)
    PendingIntent.getBroadcast(context, name.hashCode, intent,
      PendingIntent.FLAG_UPDATE_CURRENT)
  }

  def vibrator = systemService[Vibrator](Context.VIBRATOR_SERVICE)
}
