import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.web.Import._
import uk.gov.hmrc.DefaultBuildSettings

lazy val appName = "api-documentation-frontend"

Global / bloopAggregateSourceDependencies := true
Global / bloopExportJarClassifiers := Some(Set("sources"))

ThisBuild / scalaVersion := "2.13.16"
ThisBuild / majorVersion := 0
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    pipelineStages := Seq(digest)
  )
  .settings(ScoverageSettings())
  .settings(
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    scalacOptions += "-language:postfixOps"
  )
  .settings(
    routesImport ++= Seq(
      "uk.gov.hmrc.apidocumentation.controllers.binders._",
      "uk.gov.hmrc.apiplatform.modules.apis.domain.models._",
      "uk.gov.hmrc.apiplatform.modules.common.domain.models._",
      "uk.gov.hmrc.apidocumentation.models._"
    )
  )
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")
  .settings(
    Test / testOptions       += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    Test / fork              := false,
    Test / parallelExecution := false
  )
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:cat=unused&src=views/.*\\.scala:s",
      // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
      // suppress warnings in generated routes files
      "-Wconf:src=routes/.*:s"
    )
  )

lazy val acceptance = (project in file("acceptance"))
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(
    name := "acceptance-tests",
    scalacOptions += "-language:postfixOps",
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-eT"),
    Test / unmanagedResourceDirectories += baseDirectory.value / "target/web/public/test",
    DefaultBuildSettings.itSettings()
  )


commands ++= Seq(
  Command.command("cleanAll") { state => "clean" :: "acceptance/clean" :: state },
  Command.command("fmtAll") { state => "scalafmtAll" :: "acceptance/scalafmtAll" :: state },
  Command.command("fixAll") { state => "scalafixAll" :: "acceptance/scalafixAll" :: state },
  Command.command("testAll") { state => "test" :: "acceptance/test" :: state },

  Command.command("run-all-tests") { state => "testAll" :: state },
  Command.command("clean-and-test") { state => "cleanAll" :: "compile" :: "run-all-tests" :: state },
  Command.command("pre-commit") { state => "cleanAll" :: "fmtAll" :: "fixAll" :: "coverage" :: "testAll" :: "coverageOff" :: "coverageAggregate" :: state }
)
