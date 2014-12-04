import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.Deps
{
  override def deps = super.deps ++ Map(
    "droid" → common,
    "root" → common,
    "macros" → common
  )

  lazy val common = Seq(
    libraryDependencies ++= Seq(
      "com.android.support" % "support-v13" % "21.+",
      "com.google.android.gms" % "play-services" % "6.+",
      aar("org.macroid" %% "macroid" % "2.0.0-M3")
    )
  )
}

object DroidProguard
extends tryp.Proguard
{
  override lazy val cache = Seq()
  override lazy val options = Seq()
}

object DroidBuild extends tryp.MultiBuild(DroidDeps, DroidProguard) {
  override lazy val platform = "android-21"

  override lazy val settings = super.settings ++ Seq(
    scalacOptions ++= Seq("-feature", "-language:implicitConversions",
      "-deprecation"),
    scalaVersion := "2.11.2"
  )

  lazy val macros = p("macros")
    .aar
    .transitive()

  lazy val droid = p("droid")
    .aar
    .transitive
    .dep(macros)

  lazy val root = p("root")
    .path(".")
    .aar
    .transitive
    .settings(android.Plugin.androidCommands)
    .androidDeps(macros, droid)
    .aggregate(macros, droid)
}
