package tryp

import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.AndroidDeps
{
  override def deps = super.deps ++ Map(
    "core" -> core,
    "state-core" -> stateCore,
    "view-core" -> viewCore,
    "view" -> view,
    "service" -> service,
    "app" -> app,
    "test" -> test,
    "unit-core" -> unitCore,
    "unit" -> unit,
    "integration" -> integration,
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
    pulsar("slick"),
    dd("tryp" %% "pulsar-unit-core" % "+" % "test", "tek/pulsar", "unit-core")
  )

  def stateCore = ids(
    "tryp" %% "pulsar-jvm" % "+" % "test"
  )

  def viewCore = ids(
    "com.hanhuy.android" %% "iota" % "+"
  )

  def view = ids(
    aar("com.android.support" % "support-v4" % "21.+"),
    aar("com.android.support" % "appcompat-v7" % "21.+"),
    aar("com.android.support" % "palette-v7" % "21.+"),
    aar("com.android.support" % "recyclerview-v7" % "21.+"),
    aar("com.android.support" % "cardview-v7" % "21.+"),
    aar("com.android.support" % "support-v13" % "21.+"),
    aar("com.google.android.gms" % "play-services-basement" % "+"),
    aar("com.google.android.gms" % "play-services-base" % "+")
  )

  def service = ids(
    aar("com.google.android.gms" % "play-services-maps" % "+"),
    aar("com.google.android.gms" % "play-services-location" % "+"),
    aar("com.google.android.gms" % "play-services-plus" % "+")
  )

  def app = ids(
    aar("com.android.support" % "support-v13" % "21.+"),
    aar("com.android.support" % "support-v4" % "21.+"),
    "com.scalarx" %% "scalarx" % "+",
    "com.github.andkulikov" % "transitions-everywhere" % "+",
    "com.melnykov" % "floatingactionbutton" % "+",
    "com.makeramen" % "roundedimageview" % "+",
    "com.squareup.okhttp" % "okhttp" % "+"
  )

  def logback = ids(
    "com.github.tony19" % "logback-android-core" % "+" exclude(
      "com.google.android", "android"),
    "com.github.tony19" % "logback-android-classic" % "+" exclude(
      "com.google.android", "android")
  )

  def test = ids(
    pulsar("slick")
  )

  def unitCore = super.unit ++ ids(
    pulsar("unit-slick")
  )

  override def unit = ids(pulsar("jvm"))

  override def integration = super.integration ++ ids(
    "org.scalatest" %% "scalatest" % "2.2.+",
    "junit" % "junit" % "4.12" % "provided",
    "com.android.support.test" % "runner" % "+" exclude("junit", "junit"),
    "com.android.support" % "multidex-instrumentation" % "+"
  )

  def macroConsole = ids(
    pulsar("core")
  )
}
