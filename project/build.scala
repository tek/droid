package tryp

import sbt._
import sbt.Keys._

import android.Keys._
import android.protify.AndroidProtify

import org.ensime.EnsimeKeys._

import TrypAndroid.autoImport._
import TrypBuildKeys._
import Templates.autoImport._

object EnsimeFix
extends AutoPlugin
{
  def projectettings = List(
    ensimeJavacOptions in ThisBuild ++= (javacOptions in (DroidBuild.core, Compile)).value,
    ensimeScalacOptions in ThisBuild ++= (scalacOptions in (DroidBuild.core, Compile)).value
  )
}

object DroidBuild
extends tryp.AarsBuild("droid", deps = DroidDeps)
{
  val sdkVersion = 23

  override val platform = s"android-$sdkVersion"

  override def adp(name: String) = {
    super.adp(name)
      .manifest("minSdkVersion" -> sdkVersion.toString)
      .settingsV(
        setScala := true,
        fork := true,
        buildToolsVersion := Some("23.0.2"),
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

  lazy val recycler = "recycler" / "recycler view machine" << state

  lazy val api = "api" / "android api helpers" << view

  lazy val logback = "logback" / "logback deps" << view

  lazy val integrationCore = "integration-core" / "integration basics" << state

  def apk(name: String) =
    adp(name)
      .settingsV(
        aarModule := name,
        dexMaxHeap := "4G"
      )
      .logback("tag" -> "tryp") << logback

  def mkInt(name: String) =
    (apk(name) << integrationCore)
      .transitive
      .integration
      .protify
      .manifest(
        "package" -> s"tryp.droid.$name",
        "minSdk" -> sdkVersion.toString,
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

  lazy val integration = mkInt("integration") << recycler << api

  lazy val public = mpb("public")
    .settingsV(publish := (), publishLocal := ())
    .aggregate(core, viewCore, view, stateCore, state, recycler, api, logback, integrationCore)
}
