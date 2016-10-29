package tryp

import sbt._
import Keys._
import android.Keys._

object DroidProguard
extends Proguard
{
  override lazy val cache = List(
    "akka",
    "com",
    "ch.qos",
    "com.android.support",
    "com.github.amlcurran",
    "com.github.andkulikov",
    "com.github.nscala-time",
    "com.google",
    "com.makeramen",
    "com.melnykov",
    "com.robotium",
    "com.squareup",
    "effectful",
    "junit",
    "macroid",
    "monocle",
    "okio",
    "org",
    "org.joda",
    "org.slf4j",
    "org.sqldroid",
    "reactivemongo",
    "rx",
    "scala",
    "scalaz",
    "se.walkercrou",
    "shapeless",
    "slick",
    "cats",
    "dogs",
    "alleycats",
    "co.fs2",
    "io.circe",
    ""
  )

  override lazy val options = List(
    // play services stuff
    """-keep,includedescriptorclasses class * extends java.util.ListResourceBundle {
      protected java.lang.Object[][] getContents();
    }""",
    """-keep,includedescriptorclasses public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
      public static final *** NULL;
    }""",
    """-keepnames,includedescriptorclasses @com.google.android.gms.common.annotation.KeepName class *
    -keepclassmembernames,includedescriptorclasses class * {
      @com.google.android.gms.common.annotation.KeepName *;
    }""",
    """-keepnames,includedescriptorclasses class * implements android.os.Parcelable {
      public static final ** CREATOR;
    }""",
    "-dontnote com.google.android.gms.maps.internal.**",
    """-keep class com.google.android.gms.plus.PlusOneButton$OnPlusOneClickListener""",
    "-keep class shapeless.** { *; }",

    "-keepattributes Signature",
    "-keepattributes InnerClasses",
    "-keepattributes InnerClasses,EnclosingMethod",

    // slick
    "-dontwarn javax.naming.InitialContext",
    "-dontnote org.slf4j.**",
    "-dontnote javax.sql.DataSource",
    "-keep class scala.collection.List.**",
    "-keep public class org.sqldroid.**",
    "-keep class scala.concurrent.Future$.**",

    // bson
    "-dontwarn org.bson.**",

    // places-api
    "-dontwarn javax.**",
    "-dontwarn java.awt.**",
    "-dontwarn org.apache.**",

    // misc warnings
    "-dontwarn java.nio.**",
    "-dontwarn java.util.Locale",
    "-dontwarn java.util.Locale$Category",
    "-dontwarn java.beans.**",
    "-dontwarn java.net.StandardSocketOptions",
    "-dontwarn scala.collection.**",

    // misc note
    "-dontnote org.joda.time.**",
    "-dontnote android.view.GhostView",

    // test warnings
    "-keep public class * extends junit.framework.TestCase",
    "-keepclassmembers class * extends junit.framework.TestCase { *; }",
    // "-dontwarn junit.**",
    // "-dontwarn org.junit.**",
    "-dontwarn com.google.android.gms.maps.internal.**",
    "-dontwarn scala.xml.parsing.MarkupParser",
    "-dontwarn java.lang.management.**",

    // dupes
    // "-dontnote junit.**",
    "-dontnote org.json.**",
    "-dontnote org.apache.**",
    "-dontnote org.hamcrest.**",

    // showcase
    "-keep class com.github.amlcurran.showcaseview.targets.Target { *; }",
    "-dontnote com.github.amlcurran.showcaseview.**",

    // roundedimageview
    "-dontwarn com.squareup.picasso.**",

    // okhttp
    "-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement",
    "-dontnote org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement",
    "-dontnote com.android.org.conscrypt.OpenSSLSocketImpl",

    // fab
    "-dontnote com.melnykov.fab.**",

    // logback
    // "-keep class ch.qos.logback.classic.LoggerContext",
    // "-keep class ch.qos.** { *; }",
    // "-keep class org.slf4j.** { *; }",
    // "-keepattributes *Annotation*",
    "-dontnote java.nio.file.**",

    // test runner
    "-keep class com.android.test.runner.MultiDexTestRunner",
    "-keep class android.support.test.runner.AndroidJUnitRunner",

    // test stuff
    "-dontwarn org.scalatest.**",
    "-dontwarn org.specs2.**",
    "-dontwarn spray.**",
    "-dontnote android.net.http.*",
    "-dontnote com.robotium.solo.*",

    "-keep class tryp.droid.**",
    "-keep class tryp.droid.* { *; }",
    "-keep class tryp.droid.** { *; }",
    "-keep class dogs.** { *; }",
    "-keep class dogs.* { *; }",
    "-keep class cats.** { *; }",
    "-keep class cats.* { *; }",
    "-keep class alleycats.** { *; }",
    "-keep class alleycats.* { *; }",
    "-keep class scala.Function0",
    "-keep class scala.Function1",
    "-keep class scala.Function2",
    "-keep class scala.Tuple1",
    "-keep class scala.Tuple2",
    "-keep class scala.**",
    "-dontwarn org.scalacheck.**",
    "-dontwarn ch.qos.**",
    "-dontwarn org.codehaus.**",
    "-dontwarn spray.**",
    "-dontwarn akka.**",
    "-dontwarn dogs.**",
    "-dontwarn alleycats.**",
    "-dontwarn okhttp3.**",
    "-dontwarn com.google.android.gms.**",
    "-dontwarn android.security.**",
    "-dontwarn com.android.org.conscrypt.**",
    "-dontwarn sun.**",
    "-dontnote sun.**",
    "-dontwarn org.slf4j.**",
    "-dontwarn java.net.**",
    "-dontnote **",

    """-keepclasseswithmembers class * {
      native <methods>;
    }""",

    ""
  )

  override lazy val excludes = List(
    "LICENSE.txt",
    "META-INF/NOTICE.txt",
    "META-INF/LICENSE.txt"
  )

  override lazy val merges = List(
    "reference.conf"
  )
}
