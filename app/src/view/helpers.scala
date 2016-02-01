package tryp
package droid

import scala.language.dynamics
import scala.collection.convert.wrapAll._
import scala.concurrent.Await

import scalaz._
import Scalaz._

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
      val id = res.layoutId(name)
        .getOrElse(sys.error(s"invalid layout name: $name"))
      activity.getLayoutInflater.inflate(id, null) match {
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

  def hideKeyboard() = {
    Ui {
      inputMethodManager
        .hideSoftInputFromWindow(activity.root.getWindowToken, 0)
      "keyboard successfully hidden"
    }
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
    res.string("yes").foreach { text ⇒
      builder.setPositiveButton(text, new DialogListener(callback))
    }
    res.string("no").foreach { text ⇒
      builder.setNegativeButton(text, new DialogListener)
    }
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

trait Snackbars
extends HasActivity
{
  import tryp.droid.Macroid._
  import macroid.FullDsl.{toast ⇒ mToast,_}

  private implicit val ns = PrefixResourceNamespace("snackbar")

  def snackbar(resName: String) = {
    txt.content(resName).foreach(snackbarImpl)
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
    mkToast(resName).foreach(_.run)
  }

  def mkToast(resName: String) = res.s(resName).map(a ⇒ mToast(a) <~ fry)
}

trait Alarm
extends Basic
{
  import android.content.Context

  def alarmManager = systemService[AlarmManager](Context.ALARM_SERVICE)

  def scheduleRepeatingWakeAlarm[A <: WakefulBroadcastReceiver: ClassTag]
  (interval: Duration, name: String, initial: Option[Duration] = None) =
  {
    val initialInterval = (initial | 10.seconds).toMillis
    alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
      SystemClock.elapsedRealtime() + initialInterval, interval.toMillis,
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
