package tryp.droid

import scala.collection.mutable.{Map ⇒ MMap}

import android.content.SharedPreferences
import android.preference.PreferenceManager

import rx._

object PrefCaches
{
  implicit val cacheString: MMap[String, Var[String]] = MMap()

  implicit val cacheBoolean: MMap[String, Var[Boolean]] = MMap()

  implicit val cacheInt: MMap[String, Var[Int]] = MMap()
}

object PrefCache
{
  import PrefCaches._

  type Cache[A] = MMap[String, Var[A]]

  def get[A](key: String)(alt: ⇒ A)(implicit cache: Cache[A]) = {
    cache.get(key) getOrElse { set(key, alt) }
  }

  def set[A](key: String, value: A)(implicit cache: Cache[A]): Var[A] = {
    cache.get(key) tap { _() = value } getOrElse {
      Var(value) tap { v ⇒ cache(key) = v }
    }
  }

  def string(key: String)(alt: ⇒ String) = {
    get(key)(alt)
  }

  def boolean(key: String)(alt: ⇒ Boolean) = {
    get(key)(alt)
  }

  def int(key: String)(alt: ⇒ Int) = {
    get(key)(alt)
  }
}

trait Preferences
extends HasContext
{
  import PrefCaches._

  def pref(key: String, default: String = "") = {
    PrefCache.string(key) { prefs.getString(s"pref_${key}", default) }
  }

  def prefBool(key: String, default: Boolean = true) = {
    PrefCache.boolean(key) { prefs.getBoolean(s"pref_${key}", default) }
    
  }

  def prefInt(key: String, default: Int = 0) = {
    PrefCache.int(key) {
      Try { prefs.getString(s"pref_${key}", "€").toInt } match {
        case Success(value) ⇒ value
        case Failure(e) ⇒ {
          Log.e(s"Failed to convert pref '${key}' to Int: ${e}")
          default
        }
      }
    }
  }

  def prefs = PreferenceManager.getDefaultSharedPreferences(context)

  def editPrefs(callback: (SharedPreferences.Editor) ⇒ Unit,
    target: SharedPreferences = prefs
  )
  {
    val editor = target.edit
    callback(editor)
    editor.commit
  }

  def setPref(name: String, value: Any) {
    value match {
      case b: Boolean ⇒ editPrefs(_.putBoolean(name, b))
      case s: String ⇒ editPrefs(_.putString(name, s))
      case _ ⇒
        Log.e(s"Incompatible pref type ${value.className} for key '${name}'")
    }
  }

  def changePref(name: String, value: Any) {
    value match {
      case b: Boolean ⇒ updateBoolean(name, b)
      case s: String ⇒ updateString(name, s)
      case _ ⇒
        Log.e(s"Incompatible pref type ${value.className} for key '${name}'")
    }
  }

  def updateBoolean(name: String, value: Boolean) {
  }

  def updateString(name: String, value: String) {
    Try(value.toInt) match {
      case Success(int) ⇒ PrefCache.set(name, int)
      case Failure(_) ⇒ PrefCache.set(name, value)
    }
  }
}
