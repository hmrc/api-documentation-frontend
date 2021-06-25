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
import uk.gov.hmrc.PublishingSettings._
import uk.gov.hmrc.{SbtAutoBuildPlugin, _}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning

import bloop.integrations.sbt.BloopDefaults

lazy val plugins: Seq[Plugins] = Seq.empty
lazy val playSettings: Seq[Setting[_]] = Seq.empty
lazy val microservice = (project in file("."))
  .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtDistributablesPlugin) ++ plugins: _*)
  .settings(
    name := appName
  )
  .settings(
    Concat.groups := Seq(
      "javascripts/apis-app.js" -> group(
        (baseDirectory.value / "app" / "assets" / "javascripts") ** "*.js"
      )
    ),
    uglifyCompressOptions := Seq(
      "unused=true",
      "dead_code=true"
    ),
    includeFilter in uglify := GlobFilter("apis-*.js"),
    pipelineStages := Seq(digest),
    pipelineStages in Assets := Seq(
      concat,
      uglify
    )
  )
  .settings(
    resolvers ++= Seq(
      Resolver.sonatypeRepo("releases")
    ),
    resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
  )
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(ScoverageSettings(): _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    majorVersion := 0,
    scalacOptions += "-Ypartial-unification"
  )
  .settings(playPublishingSettings: _*)
  .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources")

  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .settings(
    Test / testOptions := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    Test / unmanagedSourceDirectories += baseDirectory.value / "test",
    Test / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    Test / fork := false,
    Test / parallelExecution := false
  )

  .configs(AcceptanceTest)
  .settings(inConfig(AcceptanceTest)(Defaults.testSettings): _*)
  .settings(inConfig(AcceptanceTest)(BloopDefaults.configSettings))
  .settings(
    testOptions in AcceptanceTest := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "acceptance",
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "testcommon",
    AcceptanceTest / unmanagedResourceDirectories := Seq((baseDirectory in AcceptanceTest).value / "test", (baseDirectory in AcceptanceTest).value / "target/web/public/test"),
    AcceptanceTest / fork := false,
    AcceptanceTest / parallelExecution := false,
    addTestReportOption(AcceptanceTest, "acceptance-test-reports")
  )

  .settings(scalaVersion := "2.12.12")

  .settings(SilencerSettings())

lazy val allPhases = "tt->test;test->test;test->compile;compile->compile"
lazy val AcceptanceTest = config("acceptance") extend Test
lazy val TemplateTest = config("tt") extend Test

lazy val playPublishingSettings: Seq[sbt.Setting[_]] = Seq(
  credentials += SbtCredentials,

  publishArtifact in(Compile, packageDoc) := false,
  publishArtifact in(Compile, packageSrc) := false
) ++
  publishAllArtefacts

lazy val appName = "api-documentation-frontend"
