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

  def core = ids(
    dd("tryp" %% "pulsar-slick" % "+", "tek/pulsar", "slick"),
    dd("tryp" %% "pulsar-core" % "+", "tek/pulsar", "core"),
    aar("com.android.support" % "appcompat-v7" % "21.+"),
    aar("com.android.support" % "palette-v7" % "21.+"),
    aar("com.android.support" % "recyclerview-v7" % "21.+"),
    aar("com.android.support" % "cardview-v7" % "21.+"),
    ad(aar("org.macroid" %% "macroid-core" % "+"), "macroid/macroid", "core"),
    "com.google.android.gms" % "play-services-maps" % "+",
    "com.google.android.gms" % "play-services-location" % "+",
    "com.google.android.gms" % "play-services-plus" % "+",
    "com.typesafe.akka" %% "akka-actor" % "2.3.+",
    "com.scalarx" %% "scalarx" % "0.+",
    "com.github.andkulikov" % "transitions-everywhere" % "1.+",
    "com.melnykov" % "floatingactionbutton" % "1.+",
    "com.android.support" % "support-v13" % "21.+",
    "com.makeramen" % "roundedimageview" % "2.+"
  )

  def test = ids(
    dd("tryp" %% "pulsar-test" % "+", "tek/pulsar", "test")
  )

  override def unit = super.unit ++ ids(
    dd("tryp" %% "pulsar-unit-core" % "+", "tek/pulsar", "unit-core"),
  )
}
