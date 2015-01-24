package tryp.droid

import scala.collection.convert.wrapAll._

import android.content.SharedPreferences
import android.preference.PreferenceManager

import rx._

class PreferencesFacade(val prefs: SharedPreferences)
{
  val prefix = "pref_"

  def mkKey(name: String) = {
    s"${prefix}${name.stripPrefix(prefix)}"
  }

  object PrefCaches
  {
    type Cache[A] = MMap[String, Var[A]]

    implicit val cacheString: Cache[String] = MMap()

    implicit val cacheStrings: Cache[Set[String]] = MMap()

    implicit val cacheBoolean: Cache[Boolean] = MMap()

    implicit val cacheInt: Cache[Long] = MMap()
  }

  abstract class PrefReader[A]
  {
    def apply(key: String, default: A = zero) = {
      getter(key, default)
    }

    def zero: A

    def getter: (String, A) ⇒ A
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

    implicit object `Long PR` extends PrefReader[Long] {
      def zero = 0

      def getter = (key: String, default: Long) ⇒ {
        val s = prefs.getString(key, default.toString)
        Try { s.toLong } getOrElse {
          Log.e(s"Failed to convert pref '${key}' to Long: ${s}")
          default
        }
      }
    }

    implicit object `Strings PR` extends PrefReader[Set[String]] {
      def zero = Set()

      def getter = (key: String, default: Set[String]) ⇒ {
        prefs.getStringSet(key, default).toSet
      }
    }
  }

  import PrefReaders._
  import PrefCaches._

  object PrefCache
  {
    import PrefCaches.Cache

    def get[A](name: String, default: A)(implicit cache: Cache[A],
      reader: PrefReader[A]) =
    {
      val key = mkKey(name)
      cache.getOrElseUpdate(key, Var(reader(key, default)))
    }

    def invalidate[A](name: String)(implicit cache: Cache[A],
      reader: PrefReader[A]) {
      val key = mkKey(name)
      cache.get(key) foreach { v ⇒ v() = reader(key) }
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

  def edit(callback: (SharedPreferences.Editor) ⇒ Unit)
  {
    val editor = prefs.edit
    callback(editor)
    editor.commit
  }

  def set(name: String, value: Any) {
    val key = mkKey(name)
    value match {
      case b: Boolean ⇒ edit(_.putBoolean(key, b))
      case s: String ⇒ edit(_.putString(key, s))
      case i: Int ⇒ edit(_.putString(key, i.toString))
      case l: Long ⇒ edit(_.putString(key, l.toString))
      case h: java.util.HashSet[_] ⇒ setSet(key, h.toSet)
      case s: Set[_] ⇒ setSet(key, s)
      case _ ⇒ error(key, value)
    }
    change(name, value)
  }

  private def setSet(name: String, value: Set[_]) {
    val strings = value map { _.toString }
    edit(_.putStringSet(name, strings))
  }

  def change(name: String, value: Any) {
    value match {
      case b: Boolean ⇒ PrefCache.invalidate[Boolean](name)
      case s: String ⇒ updateString(name, s)
      case i: Int ⇒ updateString(name, i.toString)
      case l: Long ⇒ updateString(name, l.toString)
      case h: java.util.HashSet[_] ⇒ PrefCache.invalidate[Set[String]](name)
      case s: Set[_] ⇒ PrefCache.invalidate[Set[String]](name)
      case _ ⇒ error(name, value)
    }
  }

  def error(name: String, value: Any) {
    val prefType = (value == null) ? "Null" / value.getClass
    Log.e(s"Incompatible pref type ${prefType} for key '${name}'")
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
    Try(value.toLong) match {
      case Success(int) ⇒ PrefCache.invalidate[Long](name)
      case Failure(_) ⇒ PrefCache.invalidate[String](name)
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
extends HasContext
{
  private def userPrefs =
    PreferenceManager.getDefaultSharedPreferences(context)

  def prefs = PrefFacades("user", userPrefs)
}

trait AppPreferences
extends HasContext
{
  private def applicationPrefs =
    context.getSharedPreferences("app_state",
      android.content.Context.MODE_PRIVATE)

  def appPrefs = PrefFacades("app", applicationPrefs)
}
