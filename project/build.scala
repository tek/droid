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

  override def adp(name: String) = {
    super.adp(name)
      .map(_.disablePlugins(CoursierPlugin))
      .manifest("minSdkVersion" -> "21")
      .settingsV(
        fork := true,
        publishArtifact in (Compile, packageDoc) := false,
        manifestTemplate := metaRes.value / "aar" / manifestName,
        packageForR := "tryp.droid.res",
        manifestTokens ++= Map(
          "targetSdk" -> sdkVersion.toString,
          "package" -> androidPackage.value,
          "versionName" -> version.value,
          "versionCode" -> "1"
        )
      )
  }

  lazy val core = "core" / "android basics"

  lazy val viewCore = "view-core" / "context abstraction core" << core

  lazy val view = "view" / "view IO streaming and iota wrappers" << viewCore

  lazy val stateCore = "state-core" / "state machine macros" << view

  lazy val state = "state" / "state machine" << stateCore

  lazy val service = "service" / "machines providing services" << state << viewCore

  lazy val app = "app" / "android commons" << view

  // lazy val db = "db" / "slick/sqldroid" << state

  lazy val logback = "logback" / "logback deps" << view

  lazy val test = "test" << app

  lazy val unitCore = ("unit-core" << test)
    .settingsV(aarModule := "unit.core")

  lazy val debug = "debug" << app

  lazy val integrationCore = "integration-core" << app

  def apk(name: String) =
    adp(name)
      .settingsV(
        aarModule := name,
        dexMaxHeap := "4G"
      )
      .logback("tag" -> "tryp") << app << logback

  def mkInt(name: String) =
    (apk(name) << view)
      .transitive
      .integration
      .protify
      .manifest(
        "package" -> s"tryp.droid.$name",
        "minSdk" -> "21",
        "activityClass" -> s"tryp.droid.$name.IntStateActivity",
        "appName" -> s"tryp $name",
        "appClass" -> s"tryp.droid.$name.IntApplication"
      )
      .settingsV(
        aarModule := name,
        logbackTemplate := metaRes.value / "integration" / logbackName,
        manifestTemplate := metaRes.value / "integration" / manifestName,
        debugIncludesTests := true
      )

  lazy val integration = mkInt("integration") << state

  lazy val unit = (tdp("unit") << unitCore << app << debug)
    .map(_.disablePlugins(CoursierPlugin))
    .settingsV(logbackTemplate := metaRes.value / "unit" / "logback.xml")
    .logback()

  lazy val trial = adp("trial")
    .protify
    .settingsV(apkbuildDebug ~= { a => a(true); a })
    .map(_.disablePlugins(CoursierPlugin))
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
      dexMulti := true,
      dexMinimizeMain := false
    )
    .logback("tag" -> "tryp") << logback

  lazy val unitDroid =
    (adp("unit-droid") << unitCore << logback << debug << view << service)
      .map(_.disablePlugins(CoursierPlugin))
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
        javaOptions += "-Drobolectric.logging=stdout"
        // ,
        // logbackOutput := outputLayout.value(projectLayout.value).classes /
        //   "assets" / logbackName
      )
      .logback("tag" -> "tryp")

  lazy val all = mpb("all")
    .aggregate(core.!, viewCore.!, view.!, state.!, service.!, app.!, logback.!, debug.!, test.!)

  override def consoleImports = """
  import cats._, data._, syntax.all._, instances.all._
  import fs2._
  import shapeless._
  import tryp._
  """
}
