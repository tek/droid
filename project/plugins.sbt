lazy val root = project.in(file(".")).dependsOn(tryp)
lazy val tryp = ProjectRef(file("../../tryp-plugin"), "android")
