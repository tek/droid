import sbt._
import sbt.Keys._
import android.Keys._

object DroidProguard
extends tryp.Proguard
{
  override lazy val cache = Seq()
  override lazy val options = Seq()
}

object DroidPlaceholders
extends tryp.Placeholders

object DroidBuild extends tryp.AndroidBuild(DroidDeps, DroidProguard,
  DroidPlaceholders) {
  override def globalSettings = super.globalSettings ++ Seq(
    lintEnabled in Android := false,
    incOptions := incOptions.value.withNameHashing(true)
  )

  lazy val trypCore = RootProject(file("../core"))

  lazy val macros = p("macros")
    .aar()

  lazy val slickCore = p("slick-core")
    .paradise()
    .antSrc
    .export
    .dep(trypCore)

  lazy val slick = p("slick")
    .paradise()
    .antSrc
    .export
    .dep(slickCore)

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
    .dep(macros, trypCore, slick, slickCore)

  lazy val test = p("test")
    .aar
    .dep(root)

  lazy val unit = p("unit")
    .aar
    .dep(test)

  lazy val debug = p("debug")
    .aar
    .dep(root)

  lazy val integration = p("integration")
    .aar
    .dep(test)
}
