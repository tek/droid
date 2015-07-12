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

  lazy val common = Seq(
    name := s"droid-${name.value}"
  )

  def default(name: String) = pb(name)
    .antSrc
    .paradise()
    .settings(common)
    .aar

  lazy val core = default("core")
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
    .transitive()

  lazy val test = default("test")
    .dep(core)

  lazy val unit = default("unit")
    .dep(test)

  lazy val debug = default("debug")
    .dep(core)

  lazy val integration = default("integration")
    .dep(test)
}
