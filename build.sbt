import android.Keys._

android.Plugin.androidBuildAar

platformTarget in Android := "android-21"

name := "droid"

description := "Common tryp stuff"

organization := "tryp"

version := "1"

scalaVersion := "2.11.2"

scalacOptions ++= Seq("-feature", "-language:implicitConversions",
  "-deprecation")

resolvers += "jcenter" at "http://jcenter.bintray.com"

libraryDependencies ++= Seq(
  "com.android.support" % "support-v13" % "21.+",
  aar("org.macroid" %% "macroid" % "2.0.0-M3")
)

transitiveAndroidLibs in Android := true

exportJars := true

addCompilerPlugin("org.brianmckenna" %% "wartremover" % "0.10")

scalacOptions in (Compile, compile) ++= (
  (dependencyClasspath in Compile).value.files.map(
    "-P:wartremover:cp:" + _.toURI.toURL
  )
)

scalacOptions in (Compile, compile) ++= Seq(
  "-P:wartremover:traverser:macroid.warts.CheckUi"
)
