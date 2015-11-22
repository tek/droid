import sbt._
import sbt.Keys._
import android.Keys._

object DroidBuild
extends tryp.AndroidBuild(deps = DroidDeps)
{
  override val title = Some("droid")

  lazy val core = aar("core")
    .settingsV(description := "android basics")

  lazy val view = aar("view")
    .settingsV(description := "iota wrappers") << core

  lazy val app = aar("app")
    .settingsV(description := "android commons")
    .transitive <<< view

  lazy val test = aar("test") <<< app

  lazy val unit = aar("unit") <<< test

  lazy val debug = aar("debug") <<< app

  lazy val integration = aar("integration") <<< test
}
