import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.uglify.Import.{uglifyCompressOptions, _}
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import sbt.Keys._
import sbt._
import uk.gov.hmrc.DefaultBuildSettings
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import bloop.integrations.sbt.BloopDefaults

Global / bloopAggregateSourceDependencies := true

scalaVersion := "2.13.8"

ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
ThisBuild / semanticdbEnabled := true
ThisBuild / semanticdbVersion := scalafixSemanticdb.revision

ThisBuild / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false)

lazy val plugins: Seq[Plugins] = Seq(PlayScala, SbtDistributablesPlugin)
lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val microservice = Project(appName, file("."))
  .enablePlugins(plugins: _*)
  .settings(
    name := appName
  )
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts" / "combine") ** "*.js"
      )
    ),
    uglifyCompressOptions := Seq(
      "unused=true",
      "dead_code=true"
    ),
    uglify / includeFilter := GlobFilter("apis-*.js"),
    pipelineStages := Seq(digest),
    Assets / pipelineStages := Seq(
      concat,
      uglify
    )
  )
  .settings(
    resolvers ++= Seq(
      Resolver.typesafeRepo("releases")
    )
  )
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(ScoverageSettings(): _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    majorVersion := 0,
    scalacOptions += "-language:postfixOps"
  )
  .settings(Compile / unmanagedResourceDirectories += baseDirectory.value / "resources")

  .settings(
    Test / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    Test / unmanagedSourceDirectories += baseDirectory.value / "test",
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    Test / fork := false,
    Test / parallelExecution := false
  )

  .configs(AcceptanceTest)
  .settings(inConfig(AcceptanceTest)(Defaults.testSettings ++ BloopDefaults.configSettings))
  .settings(
    AcceptanceTest / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "acceptance",
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    AcceptanceTest / unmanagedResourceDirectories := Seq((AcceptanceTest / baseDirectory).value / "test", (AcceptanceTest / baseDirectory).value / "target/web/public/test"),
    AcceptanceTest / fork := false,
    AcceptanceTest / parallelExecution := false,
    addTestReportOption(AcceptanceTest, "acceptance-test-reports")
  )

  .settings(DefaultBuildSettings.integrationTestSettings())

  .settings(headerSettings(AcceptanceTest) ++ automateHeaderSettings(AcceptanceTest))

  .settings(
    scalacOptions ++= Seq(
      "-Wconf:cat=unused&src=views/.*\\.scala:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s",
      "-Wconf:cat=unused&src=.*ReverseRoutes\\.scala:s"
    )
  )
lazy val AcceptanceTest = config("acceptance") extend Test

lazy val appName = "api-documentation-frontend"

  commands ++= Seq(
    Command.command("run-all-tests") { state => "test" :: "acceptance:test" :: state },

    Command.command("clean-and-test") { state => "clean" :: "compile" :: "run-all-tests" :: state },

    // Coverage does not need compile !
    Command.command("pre-commit") { state => "clean" :: "scalafmtAll" :: "scalafixAll" :: "coverage" :: "run-all-tests" :: "coverageReport" :: "coverageOff" :: state }
  )