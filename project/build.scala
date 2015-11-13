import sbt._
import sbt.Keys._
import android.Keys._

object DroidBuild
extends tryp.AndroidBuild(deps = DroidDeps)
{
  override val title = Some("droid")

  lazy val core = aar("core")
    .settingsV(description := "Common tryp stuff")
    .transitive

  lazy val test = aar("test") <<< core

  lazy val unit = aar("unit") <<< test

  lazy val debug = aar("debug") <<< core

  lazy val integration = aar("integration") <<< test
}
