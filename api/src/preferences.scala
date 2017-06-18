package tryp
package droid
package api

import scala.collection.convert.wrapAll._

import android.content.SharedPreferences
import android.preference.PreferenceManager

class PreferencesFacade(val prefs: SharedPreferences)
extends Logging
{
  val prefix = "pref_"

  def mkKey(name: String) = {
    s"${prefix}${name.stripPrefix(prefix)}"
  }

  object PrefCaches
  {
    type Cache[A] = MMap[String, A]

    implicit val cacheString: Cache[String] = MMap()

    implicit val cacheStrings: Cache[Set[String]] = MMap()

    implicit val cacheBoolean: Cache[Boolean] = MMap()

    implicit val cacheInt: Cache[Long] = MMap()

    implicit val cacheFloat: Cache[Float] = MMap()
  }

  abstract class PrefReader[A]
  {
    def apply(key: String, default: A = zero) = {
      getter(key, default)
    }

    def zero: A

    def getter: (String, A) => A
  }

  object PrefReaders
  {
    implicit object `String PR` extends PrefReader[String] {
      def zero = ""
      def getter = prefs.getString _
    }

    implicit object `Boolean PR` extends PrefReader[Boolean] {
      def zero = false
      def getter = prefs.getBoolean _
    }

    implicit object `Float PR` extends PrefReader[Float] {
      def zero = 0.0f
      def getter = prefs.getFloat _
    }

    implicit object `Long PR` extends PrefReader[Long] {
      def zero = 0

      def getter = (key: String, default: Long) => {
        val s = prefs.getString(key, default.toString)
        Try(s.toLong) getOrElse {
          log.error(s"Failed to convert pref '$key' to Long: $s")
          default
        }
      }
    }

    implicit object `Strings PR` extends PrefReader[Set[String]] {
      def zero = Set()

      def getter = (key: String, default: Set[String]) => {
        prefs.getStringSet(key, default).toSet
      }
    }
  }

  import PrefReaders._
  import PrefCaches._

  object PrefCache
  {
    import PrefCaches.Cache

    def get[A](name: String, default: A)(implicit cache: Cache[A], reader: PrefReader[A]) = {
      val key = mkKey(name)
      cache.getOrElseUpdate(key, reader(key, default))
    }

    def invalidate[A](name: String)
    (implicit cache: Cache[A], reader: PrefReader[A]) {
      val key = mkKey(name)
      cache(key) = reader(key)
    }
  }

  def string(key: String, default: String = "") = {
    PrefCache.get(key, default)
  }

  def bool(key: String, default: Boolean = true) = {
    PrefCache.get(key, default)
  }

  def int(key: String, default: Long = 0) = {
    PrefCache.get(key, default)
  }

  def strings(key: String, default: Set[String] = Set()) = {
    PrefCache.get(key, default)
  }

  def float(key: String, default: Float = 0.0f) = {
    PrefCache.get(key, default)
  }

  def edit(callback: (SharedPreferences.Editor) => Unit)
  {
    val editor = prefs.edit
    callback(editor)
    editor.commit
  }

  def set(name: String, value: Any, invalidate: Boolean = true) {
    val key = mkKey(name)
    value match {
      case b: Boolean => edit(_.putBoolean(key, b))
      case s: String => edit(_.putString(key, s))
      case i: Int => edit(_.putString(key, i.toString))
      case f: Float => edit(_.putFloat(key, f))
      case l: Long => edit(_.putString(key, l.toString))
      case h: java.util.HashSet[_] => setSet(key, h.toSet)
      case s: Set[_] => setSet(key, s)
      case t: TraversableOnce[_] => setSet(key, t.toSet)
      case _ => error(key, value)
    }
    if (invalidate) change(name, value)
  }

  private def setSet(name: String, value: Set[_]) {
    val strings = value map { _.toString }
    edit(_.putStringSet(name, strings))
  }

  def change(name: String, value: Any) {
    value match {
      case b: Boolean => PrefCache.invalidate[Boolean](name)
      case s: String => updateString(name, s)
      case i: Int => updateString(name, i.toString)
      case l: Long => updateString(name, l.toString)
      case f: Float => PrefCache.invalidate[Float](name)
      case h: java.util.HashSet[_] => PrefCache.invalidate[Set[String]](name)
      case s: Set[_] => PrefCache.invalidate[Set[String]](name)
      case t: TraversableOnce[_] => PrefCache.invalidate[Set[String]](name)
      case _ => error(name, value)
    }
  }

  def error(name: String, value: Any) {
    val prefType = if (value == null) "Null" else value.className
    log.error(s"Incompatible pref type ${prefType} for key '${name}'")
  }

  def clear() {
    edit { _.clear() }
  }

  import android.content.SharedPreferences.OnSharedPreferenceChangeListener

  def registerListener(listener: OnSharedPreferenceChangeListener) {
    prefs.registerOnSharedPreferenceChangeListener(listener)
  }

  def unregisterListener(listener: OnSharedPreferenceChangeListener) {
    prefs.unregisterOnSharedPreferenceChangeListener(listener)
  }

  private def updateString(name: String, value: String) {
    Try((value: collection.immutable.StringOps).toLong) match {
      case util.Success(int) => PrefCache.invalidate[Long](name)
      case util.Failure(_) => PrefCache.invalidate[String](name)
    }
  }
}

object PrefFacades
{
  val data: MMap[String, PreferencesFacade] = MMap()

  def apply(name: String, prefs: SharedPreferences) = {
    data.getOrElseUpdate(name, new PreferencesFacade(prefs))
  }
}

trait Preferences
{
  def context: Context

  private def userPrefs =
    PreferenceManager.getDefaultSharedPreferences(context)

  def prefs = PrefFacades("user", userPrefs)
}

trait AppPreferences
{
  def context: Context

  private def applicationPrefs =
    context.getSharedPreferences("app_state",
      android.content.Context.MODE_PRIVATE)

  def appPrefs = PrefFacades("app", applicationPrefs)
}

trait Settings
{
  def user: PreferencesFacade

  def app: PreferencesFacade
}

case class DefaultSettings()(implicit context: Context)
extends Settings
{ sett =>

  private lazy val userPrefs = new Preferences {
    def context = sett.context
  }

  private lazy val appPrefs = new AppPreferences {
    def context = sett.context
  }

  lazy val user = userPrefs.prefs

  lazy val app = appPrefs.appPrefs
}

object Settings
{
  implicit def defaultSettings(implicit context: Context) = DefaultSettings()
}

trait HasSettings
{
  implicit def settings: Settings
}

trait SettingsAIO
{
  def settingsAIO[A](f: Settings => A): AIO[A, Settings] =
    macro view.AnnotatedAIOM.inst[A, Settings]
}

// import android.content.SharedPreferences
// import android.preference.PreferenceManager
// import android.content.SharedPreferences.OnSharedPreferenceChangeListener

// trait ManagePreferences
// extends ActivityBase
// with OnSharedPreferenceChangeListener
// with Preferences
// {
//   self: MainView ⇒

//   abstract override def onCreate(state: Bundle) {
//     setupPreferences()
//     super.onCreate(state)
//   }

//   override def onSharedPreferenceChanged(prfs: SharedPreferences, key: String)
//   {
//     val value = prfs.getAll.get(key)
//     val validated = validatePreference(key, value)
//     if (validated != value) {
//       prefs.edit { editor =>
//         value match {
//           case s: String => editor.putString(key, s)
//           case b: Boolean => editor.putBoolean(key, b)
//           case _ =>
//         }
//       }
//     }
//     changePref(key, validated)
//   }

//   protected def validatePreference(key: String, value: Any): Any = {
//     value
//   }

//   abstract override def onResume() = {
//     super.onResume()
//     prefs.registerListener(this)
//   }

//   abstract override def onPause() = {
//     super.onPause()
//     prefs.unregisterListener(this)
//   }

//   def toSettings() {
//     loadFragment(Classes.fragments.settings())
//   }

//   def setupPreferences() = {
//     res.xmlId("user_preferences") foreach { id =>
//       PreferenceManager.setDefaultValues(activity, id, false)
//     }
//   }

//   def changePref(key: String, value: Any) {
//     prefs.change(key, value)
//     key match {
//       case "pref_theme" => value match {
//         case s: String => changeTheme(s)
//         case _ => prefError(key, value)
//       }
//       case _ =>
//     }
//   }

//   def changeTheme(theme: String, restart: Boolean = true): Unit

//   protected def prefError(key: String, value: Any) {
//     Log.e("Invalid value for preference ${key}: ${value} (${value.getClass})")
//   }

//   def popSettings {
//     back()
//   }
// }
