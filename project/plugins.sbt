lazy val root = project.in(file(".")).dependsOn(tryp)
lazy val tryp = file("../../tryp-plugin")
