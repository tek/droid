import sbt._
import sbt.Keys._

object Build extends android.AutoBuild
{
  lazy val macros = Project("macros", file("macros")) settings(
    scalaVersion := "2.11.2",
    version := "1"
    )
  lazy val root = Project("root", file(".")) dependsOn(macros)
}
