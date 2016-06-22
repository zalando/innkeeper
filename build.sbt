import com.typesafe.sbt.SbtScalariform.{ScalariformKeys, _}
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.dsl._
import spray.revolver.RevolverPlugin.Revolver
import io.gatling.sbt.GatlingPlugin
import scalariform.formatter.preferences.{SpacesAroundMultiImports, PreserveSpaceBeforeArguments, Preserve, DanglingCloseParenthesis, AlignSingleLineCaseStatements, DoubleIndentClassDeclaration}

name := """innkeeper"""
organization  := "org.zalando.spearheads"
version       := "0.4.0"

mainClass in Compile := Some("org.zalando.spearheads.innkeeper.Innkeeper")

scalaVersion := "2.11.8"

scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

scapegoatVersion := "1.2.1"

resolvers += "Whisk"           at "https://dl.bintray.com/whisk/maven"
resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"
resolvers += "zalando-maven"   at "https://dl.bintray.com/zalando/maven"

val akkaV            = "2.4.4"
val scalaTestV       = "3.0.0-M15"
val scalaMockV       = "3.2.2"
val slickPgV         = "0.13.0"
val slickV           = "3.1.1"

libraryDependencies ++= List(
  "org.scala-lang.modules"  %% "scala-xml"                            % "1.0.5",

  "com.typesafe.slick"      %% "slick"                                % slickV,
  "com.typesafe.slick"      %% "slick-hikaricp"                       % slickV exclude("com.zaxxer", "HikariCP-java6"),

  "com.typesafe.akka"       %% "akka-stream"                          % akkaV,
  "com.typesafe.akka"       %% "akka-http-core"                       % akkaV,
  "com.typesafe.akka"       %% "akka-http-spray-json-experimental"    % akkaV,

  "com.typesafe.akka"       %% "akka-slf4j"                           % akkaV,
  "ch.qos.logback"           % "logback-classic"                      % "1.1.7",

  "com.google.inject"        % "guice"                                % "4.0",
  "net.codingwell"          %% "scala-guice"                          % "4.0.0",
  "org.postgresql"           % "postgresql"                           % "9.4-1206-jdbc42",
  "com.github.tminglei"     %% "slick-pg"                             % slickPgV,
  "com.github.tminglei"     %% "slick-pg_date2"                       % slickPgV,
  "com.zaxxer"               % "HikariCP"                             % "2.4.5",
  "nl.grons"                %% "metrics-scala"                        % "3.5.2",

  "org.scalatest"           %% "scalatest"                            % scalaTestV       % "it,test",
  "org.scalamock"           %% "scalamock-scalatest-support"          % scalaMockV       % "it,test",
  "com.typesafe.akka"       %% "akka-http-testkit"                    % akkaV            % "it,test",
  "com.typesafe.akka"       %% "akka-stream-testkit"                  % akkaV            % "it,test",
  "io.gatling.highcharts"    % "gatling-charts-highcharts"            % "2.1.7"          % "it",
  "io.gatling"               % "gatling-test-framework"               % "2.1.7"          % "it"
)

lazy val root = project.in(file(".")).configs(IntegrationTest)
Defaults.itSettings
scalariformSettingsWithIt
Revolver.settings

enablePlugins(JavaAppPackaging, GatlingPlugin)

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
  .setPreference(PreserveSpaceBeforeArguments, true)
  .setPreference(DanglingCloseParenthesis, Preserve)
  .setPreference(DoubleIndentClassDeclaration, true)
  .setPreference(SpacesAroundMultiImports, false)
//.setPreference(AlignArguments, true)
//.setPreference(AlignParameters, true)

parallelExecution in IntegrationTest := false

fork in run := true
