import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.AndroidDeps
{
  override def deps = super.deps ++ Map(
    "root" → common,
    "slick-core" → slickCore
  )

  lazy val common = Seq(
    resolvers += "jcenter" at "http://jcenter.bintray.com",
    libraryDependencies ++= Seq(
      aar("com.android.support" % "appcompat-v7" % "21.+"),
      aar("com.android.support" % "palette-v7" % "21.+"),
      aar("com.android.support" % "recyclerview-v7" % "21.+"),
      aar("com.android.support" % "cardview-v7" % "21.+"),
      "com.google.android.gms" % "play-services-maps" % "6.+",
      "com.google.android.gms" % "play-services-location" % "6.+",
      "com.google.android.gms" % "play-services-plus" % "6.+",
      aar("org.macroid" %% "macroid" % "2.0.0-SNAPSHOT"),
      "com.typesafe.akka" %% "akka-actor" % "2.+",
      "com.scalarx" %% "scalarx" % "0.+",
      "com.github.andkulikov" % "transitions-everywhere" % "1.+",
      "com.melnykov" % "floatingactionbutton" % "1.+",
      "com.android.support" % "support-v13" % "21.+",
      "com.github.nscala-time" %% "nscala-time" % "1.+",
      "com.makeramen" % "roundedimageview" % "2.+"
    )
  )

  lazy val slickCore = Seq(
    resolvers += "Scalaz Bintray Repo" at
      "https://dl.bintray.com/scalaz/releases",
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "2.+",
      "org.slf4j" % "slf4j-nop" % "1.+",
      "joda-time" % "joda-time" % "2.+",
      "org.joda" % "joda-convert" % "1.+",
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scalaz" %% "scalaz-core" % "7.1.+",
      "org.specs2" %%  "specs2-core" % "3.+" % "test",
      "org.xerial" % "sqlite-jdbc" % "3.+" % "test",
      "io.argonaut" %% "argonaut" % "6.+" % "test"
    )
  )
}
