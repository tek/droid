package tryp

import sbt._
import sbt.Keys._

import android.Keys._
import android.protify.AndroidProtify

import TrypAndroid.autoImport._
import TrypBuildKeys._
import Templates.autoImport._

import coursier.Keys._
import coursier.CoursierPlugin

object DroidBuild
extends tryp.AarsBuild("droid", deps = DroidDeps, proguard = DroidProguard)
{
  val sdkVersion = 23

  override val platform = s"android-$sdkVersion"

  override def defaultBuilder = { name: String =>
    super.defaultBuilder(name)
      .map(_.disablePlugins(CoursierPlugin))
      .manifest("minSdkVersion" -> "21")
      .settingsV(
        fork := true,
        publishArtifact in (Compile, packageDoc) := false,
        manifestTemplate := metaRes.value / "aar" / manifestName,
        packageForR := "tryp.droid.res",
        manifestTokens ++= Map(
          "package" -> androidPackage.value,
          "versionName" -> version.value,
          "versionCode" -> "1"
        )
      )
  }

  lazy val core = "core" / "android basics"

  lazy val viewCore =
    "view-core" / "context abstraction core" <<< core

  lazy val view =
    "view" / "view IO streaming and iota wrappers" <<< viewCore

  lazy val state = "state" / "state machine" <<< view

  lazy val service =
    "service" / "machines providing services" <<< state <<< viewCore

  lazy val app =
    "app".transitive / "android commons" <<< state

  lazy val db =
    "db" / "slick/sqldroid" <<< state

  lazy val logback = "logback" / "logback deps" <<< app

  lazy val test = "test" <<< app

  lazy val unitCore = ("unit-core" <<< test)
    .settingsV(aarModule := "unit.core")

  lazy val debug = "debug" <<< app

  lazy val unitDroid =
    (adp("unit-droid") <<< unitCore <<< logback <<< debug <<< view <<< service)
      .robotest
      .manifest(
        "appName" -> "tryp",
        "appClass" -> "android.app.Application",
        "minSdk" -> "21",
        "targetSdk" -> "21",
        "versionCode" -> "1",
        "extra" -> "",
        "activityClass" -> "android.app.Activity"
      )
      .settingsV(
        manifestTemplate := metaRes.value / "unit" / manifestName,
        manifestTokens += ("package" -> androidPackage.value),
        aarModule := "unit",
        typedResources := true,
        javaOptions += "-Drobolectric.logging=stdout",
        logbackOutput := outputLayout.value(projectLayout.value).classes /
          "assets" / logbackName
      )
      .logback("tag" -> "tryp")

  lazy val unit = (tdp("unit") << unitCore << app << debug)
    .settingsV(logbackTemplate := metaRes.value / "unit" / "logback.xml")
    .logback()

  lazy val integrationCore = ("integration-core" <<< app)

  lazy val integration = (adp("integration") <<< integrationCore <<< app)
    .integration
    .protify
    .manifest(
      "package" -> "tryp.droid.integration",
      "minSdk" -> "21",
      "targetSdk" -> sdkVersion.toString,
      "activityClass" -> "tryp.droid.integration.IntStateActivity",
      "versionCode" -> "1",
      "versionName" -> "1.0",
      "appName" -> "tryp integration",
      "appClass" -> "tryp.droid.integration.IntApplication"
    )
    .settingsV(
      packageForR := "tryp.droid.res",
      aarModule := "integration",
      manifestTemplate := metaRes.value / "integration" / manifestName,
      dexMulti := true,
      dexMinimizeMain := false,
      debugIncludesTests := true
    )
    .logback("tag" -> "tryp")
    .map(_.disablePlugins(CoursierPlugin))

  lazy val trial = adp("trial")
    // .protify
    .settingsV(apkbuildDebug ~= { a ⇒ a(true); a })
    .manifest(
      "appName" -> "tryp trial",
      "appClass" -> "tryp.droid.trial.TApplication",
      "minSdk" -> "21",
      "targetSdk" -> "21",
      "activityClass" -> "tryp.droid.state.StateActivity",
      "versionCode" -> "1",
      "versionName" -> "1.0",
      "extra" -> ""
    )
    .settingsV(
      aarModule := "trial",
      manifestTemplate := metaRes.value / "trial" / manifestName,
      manifestTokens += ("package" -> androidPackage.value),
      dexMaxHeap := "4096m",
      dexMulti := true,
      dexMinimizeMain := false
    )
    .logback("tag" -> "tryp") <<< logback

  lazy val all = mpb("all")
    .aggregate(core.!, viewCore.!, view.!, state.!, service.!, app.!,
      logback.!, debug.!, test.!)

  override def consoleImports = """
  import cats._, data._, syntax.all._, instances.all._
  import scalaz.concurrent._
  import scalaz.stream._
  import Process._
  import shapeless._
  import tryp._
  """
}
