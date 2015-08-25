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

object DroidBuild
extends tryp.AndroidBuild(DroidDeps, DroidProguard, DroidPlaceholders)
{
  override def globalSettings = super.globalSettings ++ Seq(
    lintEnabled in Android := false,
    incOptions := incOptions.value.withNameHashing(true),
    organization := "tryp"
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
