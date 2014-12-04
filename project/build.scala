import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.Deps
{
  override def deps = super.deps ++ Map(
    "droid" → common
  )

  lazy val common = Seq(
    resolvers += "jcenter" at "http://jcenter.bintray.com",
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
    .aar()

  lazy val droid = p("droid")
    .aar
    .transitive
    .dep(macros)

  lazy val root = p("root")
    .path(".")
    .aar
    .transitive
    .settings(android.Plugin.androidCommands ++ Seq(
      name := "droid",
      description := "Common tryp stuff",
      organization := "tryp",
      addCompilerPlugin("org.brianmckenna" %% "wartremover" % "0.10"),
      scalacOptions in (Compile, compile) ++= (
        (dependencyClasspath in Compile).value.files.map(
          "-P:wartremover:cp:" + _.toURI.toURL
        )
      ),
      scalacOptions in (Compile, compile) ++= Seq(
        "-P:wartremover:traverser:macroid.warts.CheckUi"
      )
    ))
    .androidDeps(macros, droid)
    .aggregate(macros, droid)
}
