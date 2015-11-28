package tryp

import sbt._
import sbt.Keys._
import android.Keys._
import TrypAndroid.autoImport._
import Tryp.autoImport._
import Templates.autoImport._

object DroidBuild
extends tryp.AarsBuild("droid", deps = DroidDeps)
{
  lazy val core = "core" / "android basics"

  lazy val view = "view" / "iota wrappers" <<< core

  lazy val app = "app".transitive / "android commons" <<< view

  lazy val logback = "logback" / "logback deps" <<< app

  lazy val test = "test" <<< app

  lazy val unitCore = "unit-core" <<< test

  lazy val debug = "debug" <<< app

  lazy val unitDroid = (adp("unit-droid") <<< unitCore <<< logback <<< debug)
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

  lazy val unit = (tdp("unit") << unitCore << app << debug)
    .settingsV(logbackTemplate := metaRes.value / "unit" / "logback.xml")
    .logback()

  lazy val integration = "integration" <<< test

  override def consoleImports = super.consoleImports + """
  import concurrent._
  import stream._
  import Process._
  import scala.concurrent.duration._
  """
}
