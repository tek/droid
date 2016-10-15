package tryp

import sbt._
import sbt.Keys._

import android.Keys._

import TrypAndroid.autoImport._
import TrypBuildKeys._
import Templates.autoImport._

import coursier.Keys._
import coursier.CoursierPlugin

object DroidBuild
extends tryp.AarsBuild("droid", deps = DroidDeps, proguard = DroidProguard)
with tryp.ToTransformIf
{
  override val platform = "android-23"

  override def defaultBuilder = { name: String =>
    super.defaultBuilder(name)
      .map(_.disablePlugins(CoursierPlugin))
      .manifest("minSdkVersion" -> "14")
      .settingsV(
        fork := true,
        publishArtifact in (Compile, packageDoc) := false,
        manifestTemplate := metaRes.value / "aar" / manifestName,
        typedResources := true,
        packageForR := "tryp.droid.res",
        resolvers +=
          Resolver.url(s"pulsar maven 2", url(s"${Tek.pulsarUri}/snapshots"))(Patterns(true, nexusPattern)),
        manifestTokens ++= Map(
          "package" -> androidPackage.value,
          "versionName" -> version.value,
          "versionCode" -> "1"
        )
      )
  }

  lazy val core = "core" / "android basics"

  lazy val viewCore = "view-core" / "context abstraction core" <<< core

  lazy val view =
    "view" / "view IO streaming and iota wrappers" <<< viewCore

  lazy val state = "state" / "state machine" <<< view

  lazy val service =
    "service" / "machines providing services" <<< state <<< viewCore

  lazy val app =
    "app".multidexDeps / "android commons" <<< service

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
        packageForR := "tryp.droid.res",
        typedResources := true,
        javaOptions += "-Drobolectric.logging=stdout",
        logbackOutput := outputLayout.value(projectLayout.value).classes /
          "assets" / logbackName
      )
      .logback("tag" -> "tryp")
      .map(_.disablePlugins(CoursierPlugin))

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
      packageForR := "tryp.droid.res",
      typedResources := true,
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
