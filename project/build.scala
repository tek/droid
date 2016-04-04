package tryp

import sbt._
import sbt.Keys._

import android.Keys._

import TrypAndroid.autoImport._
import TrypBuildKeys._
import Templates.autoImport._

object DroidBuild
extends tryp.AarsBuild("droid", deps = DroidDeps, proguard = DroidProguard)
{
  override def defaultBuilder = { name: String =>
    super.defaultBuilder(name)
      .manifest("minSdkVersion" -> "14")
      .settingsV(
        fork := true,
        manifestTemplate := metaRes.value / "aar" / manifestName,
        manifestTokens ++= Map(
          "package" -> androidPackage.value,
          "versionName" -> version.value,
          "versionCode" -> "1"
        )
      )
  }

  lazy val core = "core" / "android basics"

  lazy val viewCore = "view-core" / "context abstraction core" <<< core

  lazy val stateCore = ("state-core" / "state machine core" <<< core)
    .logback("tag" -> "tryp")
    .settingsV(
      logbackTemplate := metaRes.value / "unit" / "logback.xml",
      generateLogback := false,
      generateLogback in Test := true
    )

  lazy val view = 
    "view" / "view IO streaming and iota wrappers" <<< viewCore <<< stateCore

  lazy val state = "state" / "state machine" <<< view

  lazy val service = 
    "service" / "machines providing services" <<< state

  lazy val app =
    "app".multidexDeps / "android commons" <<< service

  lazy val logback = "logback" / "logback deps" <<< app

  lazy val test = "test" <<< app

  lazy val unitCore = ("unit-core" <<< test)
    .settingsV(aarModule := "unit.core")

  lazy val debug = "debug" <<< app

  lazy val unitDroid = (adp("unit-droid") <<< unitCore <<< logback <<< debug)
    .robotest
    .manifest(
      "appName" -> "tryp",
      "appClass" -> ".Application",
      "minSdk" -> "21",
      "targetSdk" -> "21",
      "versionCode" -> "1",
      "extra" -> "",
      "activityClass" -> ".SpecActivity"
    )
    .settingsV(
      manifestTokens += ("package" -> androidPackage.value),
      aarModule := "unit",
      logbackOutput := outputLayout.value(projectLayout.value).classes /
        "assets" / logbackName
    )
    .logback("tag" -> "tryp")

  lazy val unit = (tdp("unit") << unitCore << app << debug)
    .settingsV(logbackTemplate := metaRes.value / "unit" / "logback.xml")
    .logback()

  lazy val integration = "integration" <<< test

  lazy val trial = adp("trial")
    .protify
    .settingsV(apkbuildDebug ~= { a ⇒ a(true); a })
    .manifest(
      "appName" -> "tryp trial",
      "appClass" -> "tryp.droid.trial.TApplication",
      "minSdk" -> "22",
      "targetSdk" -> "22",
      "activityClass" -> "tryp.droid.state.StateActivity",
      "versionCode" -> "1",
      "versionName" -> "1.0",
      "extra" -> ""
    )
    .settingsV(
      aarModule := "trial",
      manifestTemplate := metaRes.value / "trial" / manifestName,
      manifestTokens += ("package" -> androidPackage.value),
      dexMaxHeap := "2048m"
    )
    .logback("tag" -> "tryp")
    .multidex(
      "tryp/droid/trial/TApplication.class",
      "tryp/droid/state/StateActivity.class"
    ) <<< logback


  override def consoleImports = """
  import cats._, data._, syntax.all._, std.all._
  import scalaz.concurrent._
  import scalaz.stream._
  import Process._
  import shapeless._
  import tryp._
  """
}
