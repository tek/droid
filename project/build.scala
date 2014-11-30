import sbt._
import sbt.Keys._

object DroidBuild extends Build
{
  lazy val macros = Project("macros", file("macros")) settings(
    scalaVersion := "2.11.2",
    version := "1"
    )
  lazy val root = Project("root", file(".")) dependsOn(macros)
}
