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
  .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
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
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo,
      Resolver.sonatypeRepo("releases")
    ),
    resolvers += "hmrc-releases" at "https://artefacts.tax.service.gov.uk/artifactory/hmrc-releases/"
  )
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= appDependencies,
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
    Test / fork := false,
    Test / parallelExecution := false
  )

  .configs(AcceptanceTest)
  .settings(inConfig(AcceptanceTest)(Defaults.testSettings): _*)
  .settings(inConfig(AcceptanceTest)(BloopDefaults.configSettings))
  .settings(
    testOptions in AcceptanceTest := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "acceptance",
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
lazy val appDependencies: Seq[ModuleID] = allDeps

lazy val compile = Seq(
  ws,
  ehcache,
  "uk.gov.hmrc"                           %% "bootstrap-play-26"      % "2.2.0",
  "uk.gov.hmrc"                           %% "url-builder"            % "3.4.0-play-26",
  "uk.gov.hmrc"                           %% "http-metrics"           % "1.11.0",
  "uk.gov.hmrc"                           %% "govuk-template"         % "5.61.0-play-26",
  "uk.gov.hmrc"                           %% "play-ui"                % "8.21.0-play-26",
  "uk.gov.hmrc"                           %% "play-partials"          % "7.1.0-play-26",
  "uk.gov.hmrc"                           %% "play-frontend-govuk"    % "0.60.0-play-26",
  "uk.gov.hmrc"                           %% "play-frontend-hmrc"     % "0.38.0-play-26",
  "io.dropwizard.metrics"                 % "metrics-graphite"        % "3.2.0",
  "org.typelevel"                         %% "cats-core"              % "2.0.0",
  "org.commonjava.googlecode.markdown4j"  % "markdown4j"              % "2.2-cj-1.1",
  "com.typesafe.play"                     %% "play-json"              % "2.8.1"
)

lazy val testScopes = "test"

lazy val test = Seq(
  "io.cucumber"                           %% "cucumber-scala"         % "5.7.0" % testScopes,
  "io.cucumber"                           % "cucumber-junit"          % "5.7.0" % testScopes,
  "uk.gov.hmrc"                           %% "hmrctest"               % "3.10.0-play-26" % testScopes,
  "org.pegdown"                           % "pegdown"                 % "1.6.0" % testScopes,
  "com.typesafe.play"                     %% "play-test"              % PlayVersion.current % testScopes,
  "org.scalatestplus.play"                %% "scalatestplus-play"     % "3.1.3" % testScopes,
  "org.mockito"                           % "mockito-core"            % "1.10.19" % testScopes,
  "org.seleniumhq.selenium"               % "selenium-java"           % "3.141.59" % testScopes,
  "org.seleniumhq.selenium"               % "selenium-firefox-driver" % "3.141.59",
  "org.seleniumhq.selenium"               % "selenium-chrome-driver"  % "3.141.59",
  "com.github.tomakehurst"                % "wiremock"                % "1.58" % testScopes,
  "org.jsoup"                             % "jsoup"                   % "1.12.1" % testScopes
)

lazy val allDeps = compile ++ test

val ScoverageExclusionPatterns = List(
  "<empty>",
  "definition.*",
  "sandbox.*",
  "live.*",
  "prod.*",
  "testOnlyDoNotUseInAppConf.*",
  "uk.gov.hmrc.config.*",
  "app.Routes",
  "app.RoutesPrefix",
  "controllers.javascript",
  "com.kenshoo.play.metrics.javascript",
  "com.kenshoo.play.metrics",
  ".*Reverse.*",
  "uk.gov.hmrc.controllers.Reverse*",
)

// Coverage configuration
// TODO ebridge - Fix and set back to 85
coverageMinimum := 80
coverageFailOnMinimum := true
coverageExcludedPackages := ScoverageExclusionPatterns.mkString("",";","")
