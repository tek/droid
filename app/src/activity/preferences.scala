// package tryp.droid

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
