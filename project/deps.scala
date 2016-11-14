package tryp

import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.AndroidDeps
{
  override def deps = super.deps ++ Map(
    "core" -> core,
    "view-core" -> viewCore,
    "view" -> view,
    "service" -> service,
    "app" -> app,
    "db" -> db,
    "test" -> test,
    "unit-core" -> unitCore,
    "unit-droid" -> unitDroid,
    "unit" -> unit,
    "integration-core" -> integrationCore,
    "logback" -> logback,
    "macro-console" -> macroConsole
  )

  override def resolvers = Map(
    "unit-core" -> List(Resolver.bintrayRepo("tek", "releases")),
    "app" -> List(Resolver.jcenterRepo)
  )

  def pulsar(pro: String) = {
    dd("tryp" %% s"pulsar-$pro" % "+", "tek/pulsar", pro)
  }

  def core = ids(
    pulsar("main")
    // "tryp" %% "pulsar-unit-core" % "+" % "test"
  )

  def viewCore = ids(
    "com.hanhuy.android" %% "iota" % "2.0.0-SNAPSHOT"
  )

  def view = ids(
    pulsar("state"),
    aar("com.android.support" % "support-v4" % "23.+"),
    aar("com.android.support" % "appcompat-v7" % "23.+"),
    aar("com.android.support" % "palette-v7" % "23.+"),
    aar("com.android.support" % "recyclerview-v7" % "23.+"),
    aar("com.android.support" % "cardview-v7" % "23.+"),
    aar("com.android.support" % "support-v13" % "23.+")
  )

  def service = ids(
    aar("com.google.android.gms" % "play-services-maps" % "9.+"),
    aar("com.google.android.gms" % "play-services-location" % "9.+"),
    aar("com.google.android.gms" % "play-services-plus" % "9.+")
  )

  def app = ids(
    "com.lihaoyi" %% "scalarx" % "0.2.8",
    "com.melnykov" % "floatingactionbutton" % "1.+",
    // "com.makeramen" % "roundedimageview" % "1.+",
    "com.squareup.okhttp3" % "okhttp" % "3.+"
  )

  def db = ids(
    pulsar("slick")
  )

  def logback = ids(
    "com.github.tony19" % "logback-android-core" % "1.+" exclude(
      "com.google.android", "android"),
    "com.github.tony19" % "logback-android-classic" % "1.+" exclude(
      "com.google.android", "android")
  )

  def test = ids(
  )

  def unitCore = ids(
    "tryp" %% "speclectic" % "1.0.1"
  )

  override def unit = ids(pulsar("jvm"))

  def unitDroid = ids(
    "org.xerial" % "sqlite-jdbc" % "3.+" % "test"
  )

  def integrationCore = super.integration ++ ids(
    "junit" % "junit" % "4.12" % "provided",
    "com.android.support.test" % "runner" % "0.+" exclude("junit", "junit"),
    "com.android.support" % "multidex-instrumentation" % "1.+"
  )

  def macroConsole = ids(
    pulsar("core")
  )
}
