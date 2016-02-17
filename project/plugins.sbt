resolvers += Classpaths.sbtPluginReleases
resolvers += "Sonatype OSS Releases" at "https://oss.sonatype.org/service/local/staging/deploy/maven2"

addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
// commented for now, as it add unwanted dependencies to the final jar
//addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.0.4")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.2.0")
addSbtPlugin("io.spray" %% "sbt-revolver" % "0.7.2")
addSbtPlugin("com.typesafe.sbt" %% "sbt-native-packager" % "1.0.2")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.1")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.8.2")