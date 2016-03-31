// package tryp
// package droid

// import java.io.File

// import android.content.pm.ApplicationInfo
// import android.support.multidex.MultiDex

// import tryp.slick.DroidDbInfo

// import tryp.core._

// trait TrypApplication
// extends HasContext
// with ApplicationI
// { self: android.app.Application =>

//   val useDb = true

//   def setupDbInfo(name: String) = {
//     val dbPath = new File(context.getFilesDir, s"$name.db")
//     DbMeta.setDbInfo(DroidDbInfo(dbPath.toString))
//   }

//   def isDebug = {
//     (getApplicationInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0
//   }

//   def setupEnv() = {
//     if (isDebug) setEnv(DebugEnv)
//   }

//   def setupLog(name: String) = {
//     Logs.log =
//       if (TrypEnv.release) tryp.droid.meta.InternalLog
//       else if (TrypEnv.unittest) tryp.StdoutLog
//       else tryp.droid.meta.DebugLog
//     AndroidLog.tag = name
//   }

//   def createTrypApp(name: String) {
//     setupEnv()
//     setupLog(name)
//     if (useDb) setupDbInfo(name)
//   }
// }

// trait Application
// extends TrypApplication
// {
//   self: android.app.Application =>

//   def context = getApplicationContext

//   def name: String

//   abstract override def onCreate() {
//     createTrypApp(name)
//     super.onCreate()
//   }
// }

// trait MultiDexApplication
// extends ApplicationI
// {
//   self: Application with android.app.Application =>

//     abstract override def attachBaseContext(base: Context) {
//       super.attachBaseContext(base)
//       MultiDex.install(this)
//     }
// }
