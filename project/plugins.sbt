resolvers ++=
  Resolver.url("bintray-tek-sbt-plugins",
  url("https://dl.bintray.com/tek/sbt-plugins"))(Resolver.ivyStylePatterns) ::
  Nil

addSbtPlugin("tryp.sbt" % "tryp-android" % "0.5")
