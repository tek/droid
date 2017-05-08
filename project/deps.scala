package tryp

import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.AndroidDeps
{
  val trypV = "+"

  override def deps = super.deps ++ Map(
    "core" -> core,
    "view-core" -> viewCore,
    "view" -> view,
    "state-core" -> state,
    "service" -> service,
    "app" -> app,
    "db" -> db,
    "test" -> test,
    "unit-core" -> unitCore,
    "unit-droid" -> unitDroid,
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
    dd("tryp" %% s"pulsar-$pro" % trypV, "tek/pulsar", pro)
    // "tryp" %% s"pulsar-$pro" % trypV
  }

  def core = ids(
    // "codes.reactive" %% "scala-time" % "0.4.1",
    pulsar("main")
  )

  def viewCore = ids(
    "com.hanhuy.android" %% "iota" % "2.0.0-SNAPSHOT"
  )

  val supportV = "23.4.0"

  def view = ids(
    "com.android.support" % "support-v4" % supportV,
    "com.android.support" % "appcompat-v7" % supportV,
    "com.android.support" % "palette-v7" % supportV,
    "com.android.support" % "recyclerview-v7" % supportV,
    "com.android.support" % "cardview-v7" % supportV,
    "com.android.support" % "support-v13" % supportV
  )

  def state = ids(
    pulsar("state-reflect")
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
  ) ++ view

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

  override def integration = super.integration ++ ids(
    // "junit" % "junit" % "4.12" exclude("junit", "junit"),
    // "com.android.support.test" % "runner" % "0.5",
    // "com.android.support.test" % "rules" % "0.5"
  )

  def macroConsole = ids(
    pulsar("core")
  )
}
