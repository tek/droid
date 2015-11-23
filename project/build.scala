package tryp

import sbt._
import sbt.Keys._
import android.Keys._
import TrypAndroid.autoImport._
import Tryp.autoImport._

object DroidBuild
extends tryp.AarsBuild("droid", deps = DroidDeps)
{
  lazy val core = "core" / "android basics"

  lazy val view = "view" / "iota wrappers" <<< core

  lazy val app = "app".transitive / "android commons" <<< view

  lazy val logback = "logback" / "logback deps" <<< app

  lazy val test = "test" <<< app

  lazy val unitCore = "unit-core" <<< test

  lazy val unit = (adp("unit") <<< unitCore <<< app)
    .robotest
    .manifest(
      "appName" → "tryp",
      "minSdk" → "21",
      "targetSdk" → "21",
      "versionCode" → "1",
      "extra" → "",
      "activityClass" → ".SpecActivity"
    )
    .settingsV(
      manifestTokens += ("package" → androidPackage.value),
      aarModule := "unit",
      logbackOutput := outputLayout.value(projectLayout.value).classes /
        "assets" / logbackName
    )
    .logback("tag" → "tryp")

  lazy val debug = "debug" <<< app

  lazy val integration = "integration" <<< test
}
