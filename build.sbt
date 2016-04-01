import com.typesafe.sbt.SbtScalariform.{ScalariformKeys, _}
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import sbt.dsl._
import spray.revolver.RevolverPlugin.Revolver
import io.gatling.sbt.GatlingPlugin
import scalariform.formatter.preferences.{SpacesAroundMultiImports, PreserveSpaceBeforeArguments, Preserve, Prevent, DanglingCloseParenthesis, AlignArguments, AlignSingleLineCaseStatements, DoubleIndentClassDeclaration, AlignParameters}

name := """innkeeper"""
organization  := "org.zalando.spearheads"
version       := "0.0.1"

mainClass in Compile := Some("org.zalando.spearheads.innkeeper.Innkeeper")

scalaVersion := "2.11.7"

scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-encoding", "utf8")

resolvers += "Whisk" at "https://dl.bintray.com/whisk/maven"
resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"
resolvers += "Clojars" at "http://clojars.org/repo/"

val akkaStreamV      = "2.4.2"
val scalaTestV       = "3.0.0-M15"
val scalaMockV       = "3.2.2"
val slickPgV         = "0.11.2"
val slickV           = "3.1.1"

libraryDependencies ++= List(
  "org.scala-lang.modules"  %% "scala-xml"                            % "1.0.4",
  
  "com.typesafe.slick"      %% "slick"                                % slickV,
  "com.typesafe.slick"      %% "slick-hikaricp"                       % slickV exclude("com.zaxxer", "HikariCP-java6"),

  "com.typesafe.akka"       %% "akka-stream"                          % akkaStreamV,
  "com.typesafe.akka"       %% "akka-http-core"                       % akkaStreamV,
  "com.typesafe.akka"       %% "akka-http-spray-json-experimental"    % akkaStreamV,

  "com.typesafe.akka"       %% "akka-slf4j"                           % "2.3.9",
  "ch.qos.logback"           % "logback-classic"                      % "1.1.3",
  
  "org.clojars.danpersa"     % "instaskip"                            % "0.1.7",

  "com.google.inject"        % "guice"                                % "4.0",
  "net.codingwell"          %% "scala-guice"                          % "4.0.0",
  "org.postgresql"           % "postgresql"                           % "9.4-1206-jdbc42",
  "com.github.tminglei"     %% "slick-pg"                             % slickPgV,
  "com.github.tminglei"     %% "slick-pg_date2"                       % slickPgV,
  "com.zaxxer"               % "HikariCP"                             % "2.4.3",
  "nl.grons"                %% "metrics-scala"                        % "3.5.2",

  "org.scalatest"           %% "scalatest"                            % scalaTestV       % "it,test",
  "org.scalamock"           %% "scalamock-scalatest-support"          % scalaMockV       % "it,test",
  "com.typesafe.akka"       %% "akka-http-testkit"                    % akkaStreamV      % "it,test",
  "com.typesafe.akka"       %% "akka-stream-testkit"                  % akkaStreamV      % "it,test",
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
