import sbt._
import sbt.Keys._
import android.Keys._

object DroidBuild
extends tryp.AndroidBuild(deps = DroidDeps)
{
  override def globalSettings = super.globalSettings ++ Seq(
    typedResources in Android := false,
    lintEnabled in Android := false,
    organization := "tryp",
    scalacOptions += "-target:jvm-1.7"
  )

  override val prefix = Some("droid")

  def ddp(name: String) = tdp(name).aar

  lazy val core = ddp("core")
    .settingsV(description := "Common tryp stuff")
    .transitive()

  lazy val test = ddp("test")
    .dep(core)

  lazy val unit = ddp("unit")
    .dep(test)

  lazy val debug = ddp("debug")
    .dep(core)

  lazy val integration = ddp("integration")
    .dep(test)
}
