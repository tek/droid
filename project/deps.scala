import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.AndroidDeps
{
  override def deps = super.deps ++ Map(
    "core" → core,
    "test" → test,
    "unit" → unit
  )

  override def resolvers = super.resolvers ++ Map(
    "core" → Seq(Resolver.jcenterRepo)
  )

  def pulsar(pro: String) = {
    dd("tryp" %% s"pulsar-$pro" % "+", "tek/pulsar", pro)
  }

  def core = ids(
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
    ad(aar("org.macroid" %% "macroid" % "2.0.0-M4"), "macroid/macroid", "core").no,
    "com.typesafe.akka" %% "akka-actor" % "2.3.+",
    "com.scalarx" %% "scalarx" % "+",
    "com.github.andkulikov" % "transitions-everywhere" % "+",
    "com.melnykov" % "floatingactionbutton" % "+",
    "com.android.support" % "support-v13" % "21.+",
    "com.makeramen" % "roundedimageview" % "+",
    "com.squareup.okhttp" % "okhttp" % "+"
  )

  def test = ids(
    pulsar("unit-slick")
  )
}
