package tryp.droid

import scala.collection.mutable.{Map ⇒ MMap}
import scala.collection.convert.wrapAll._

import android.content.SharedPreferences
import android.preference.PreferenceManager

import rx._

class PreferencesFacade(prefs: SharedPreferences)
{

  object PrefCaches
  {
    type Cache[A] = MMap[String, Var[A]]

    implicit val cacheString: Cache[String] = MMap()

    implicit val cacheStrings: Cache[Set[String]] = MMap()

    implicit val cacheBoolean: Cache[Boolean] = MMap()

    implicit val cacheInt: Cache[Int] = MMap()
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

    implicit object `Int PR` extends PrefReader[Int] {
      def zero = 0

      def getter = (key: String, default: Int) ⇒ {
        val s = prefs.getString(key, default.toString)
        Try { s.toInt } getOrElse {
          Log.e(s"Failed to convert pref '${key}' to Int: ${s}")
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

    val prefix = "pref_"

    def mkKey(name: String) = {
      s"${prefix}${name.stripPrefix(prefix)}"
    }
  }

  def string(key: String, default: String = "") = {
    PrefCache.get(key, default)
  }

  def bool(key: String, default: Boolean = true) = {
    PrefCache.get(key, default)
  }

  def int(key: String, default: Int = 0) = {
    PrefCache.get(key, default)
  }

  def strings(key: String, default: Set[String] = Set()) = {
    PrefCache.get(key, default)
  }

  def edit(callback: (SharedPreferences.Editor) ⇒ Unit,
    target: SharedPreferences = prefs
  )
  {
    val editor = target.edit
    callback(editor)
    editor.commit
  }

  def set(name: String, value: Any) {
    value match {
      case b: Boolean ⇒ edit(_.putBoolean(name, b))
      case s: String ⇒ edit(_.putString(name, s))
      case _ ⇒
        Log.e(s"Incompatible pref type ${value.getClass} for key '${name}'")
    }
  }

  def change(name: String, value: Any) {
    value match {
      case b: Boolean ⇒ PrefCache.invalidate[Boolean](name)
      case s: String ⇒ updateString(name, s)
      case h: java.util.HashSet[_] ⇒ PrefCache.invalidate[Set[String]](name)
      case _ ⇒
        Log.e(s"Incompatible pref type ${value.getClass} for key '${name}'")
    }
  }

  import android.content.SharedPreferences.OnSharedPreferenceChangeListener

  def registerListener(listener: OnSharedPreferenceChangeListener) {
    prefs.registerOnSharedPreferenceChangeListener(listener)
  }

  def unregisterListener(listener: OnSharedPreferenceChangeListener) {
    prefs.unregisterOnSharedPreferenceChangeListener(listener)
  }

  private def updateString(name: String, value: String) {
    Try(value.toInt) match {
      case Success(int) ⇒ PrefCache.invalidate[Int](name)
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
extends view.HasActivity
{
  private def appPrefs =
    activity.getPreferences(android.content.Context.MODE_PRIVATE)

  def prefs = PrefFacades("app", appPrefs)
}
