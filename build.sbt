import _root_.play.core.PlayVersion
import _root_.play.sbt.PlayImport._
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.uglify.Import.{uglifyCompressOptions, _}
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings._
import uk.gov.hmrc.{SbtAutoBuildPlugin, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import scala.util.Properties
import bloop.integrations.sbt.BloopDefaults

Global / bloopAggregateSourceDependencies := true

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin) ++ plugins: _*)
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
      Resolver.sonatypeRepo("releases")
    )
  )
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(ScoverageSettings(): _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    update / evictionWarningOptions := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    majorVersion := 0,
    scalacOptions += "-Ypartial-unification"
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

  .settings(scalaVersion := "2.12.15")

  .settings(SilencerSettings())

lazy val AcceptanceTest = config("acceptance") extend Test

lazy val appName = "api-documentation-frontend"
