import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.AndroidDeps
{
  override def deps = super.deps ++ Map(
    "core" → core,
    "view" → view,
    "app" → app,
    "test" → test,
    "unit" → unit,
    "integration" → integration
  )

  override def resolvers = super.resolvers ++ Map(
    "app" → List(Resolver.jcenterRepo)
  )

  def pulsar(pro: String) = {
    dd("tryp" %% s"pulsar-$pro" % "+", "tek/pulsar", pro)
  }

  def core = ids(
    pulsar("macros")
  )

  def view = ids(
    "com.hanhuy.android" %% "iota" % "+"
  )

  def app = ids(
    pulsar("slick"),
    "com.github.tony19" % "logback-android-core" % "+" exclude(
      "com.google.android", "android"),
    "com.github.tony19" % "logback-android-classic" % "+" exclude(
      "com.google.android", "android"),
    aar("com.android.support" % "appcompat-v7" % "21.+"),
    aar("com.android.support" % "palette-v7" % "21.+"),
    aar("com.android.support" % "recyclerview-v7" % "21.+"),
    aar("com.android.support" % "cardview-v7" % "21.+"),
    "com.google.android.gms" % "play-services-maps" % "+",
    "com.google.android.gms" % "play-services-location" % "+",
    "com.google.android.gms" % "play-services-plus" % "+",
    ad(aar("org.macroid" %% "macroid" % "2.0.0-M4"), "macroid/macroid",
      "core").no,
    "com.typesafe.akka" %% "akka-actor" % "2.3.+",
    "com.scalarx" %% "scalarx" % "+",
    "com.github.andkulikov" % "transitions-everywhere" % "+",
    "com.melnykov" % "floatingactionbutton" % "+",
    "com.android.support" % "support-v13" % "21.+",
    "com.makeramen" % "roundedimageview" % "+",
    "com.squareup.okhttp" % "okhttp" % "+"
  )

  def test = ids(
    pulsar("slick")
  )

  override def unit = super.unit ++ ids(
    pulsar("unit-slick")
  )

  override def integration = super.integration ++ ids(
    "org.scalatest" %% "scalatest" % "2.2.+",
    "junit" % "junit" % "4.12" % "provided",
    "com.android.support.test" % "runner" % "+" exclude("junit", "junit"),
    "com.android.support" % "multidex-instrumentation" % "+"
  )
}
