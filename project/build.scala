import sbt._
import sbt.Keys._
import android.Keys._

object DroidDeps
extends tryp.Deps
{
  override def deps = super.deps ++ Map(
    "root" → common,
    "slickmacros" → slickmacros
  )

  lazy val common = Seq(
    resolvers += "jcenter" at "http://jcenter.bintray.com",
    libraryDependencies ++= Seq(
      aar("com.android.support" % "appcompat-v7" % "21.+"),
      aar("com.android.support" % "palette-v7" % "21.+"),
      aar("com.android.support" % "recyclerview-v7" % "21.+"),
      aar("com.android.support" % "cardview-v7" % "21.+"),
      "com.google.android.gms" % "play-services" % "6.+",
      aar("org.macroid" %% "macroid" % "2.0.0-SNAPSHOT"),
      "com.typesafe.akka" %% "akka-actor" % "2.3.3",
      "org.scalaz" %% "scalaz-core" % "7.+",
      "com.melnykov" % "floatingactionbutton" % "1.+",
      "com.android.support" % "support-v13" % "21.+"
    )
  )

  lazy val slickmacros = Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.slick" %% "slick" % "2.+",
      "org.slf4j" % "slf4j-nop" % "1.+",
      "joda-time" % "joda-time" % "2.+",
      "org.joda" % "joda-convert" % "1.+",
      "org.scala-lang" % "scala-reflect" % "2.11.4",
      "org.scala-lang" % "scala-compiler" % "2.11.4"
    )
  )
}

object DroidProguard
extends tryp.Proguard
{
  override lazy val cache = Seq()
  override lazy val options = Seq()
}

object DroidPlaceholders
extends tryp.Placeholders

object DroidBuild extends tryp.MultiBuild(DroidDeps, DroidProguard,
  DroidPlaceholders) {
  override lazy val platform = "android-21"

  override lazy val settings = super.settings ++ Seq(
    scalacOptions ++= Seq("-feature", "-language:implicitConversions",
      "-deprecation", "-language:postfixOps", "-language:reflectiveCalls"),
    scalaVersion := "2.11.4"
  )

  lazy val macros = p("macros")
    .aar()

  lazy val slickmacros = p("slickmacros")
    .paradise()
    .antSrc
    .export()

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
    .dep(macros)

  lazy val unit = p("unit")
    .aar
    .dep(root)

  lazy val integration = p("integration")
    .aar
    .dep(root)
}
