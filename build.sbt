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
    majorVersion := 0
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
  .settings(
    testOptions in AcceptanceTest := Seq(Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    AcceptanceTest / unmanagedSourceDirectories += baseDirectory.value / "acceptance",
    AcceptanceTest / unmanagedResourceDirectories := Seq((baseDirectory in AcceptanceTest).value / "test", (baseDirectory in AcceptanceTest).value / "target/web/public/test"),
    AcceptanceTest / fork := false,
    AcceptanceTest / parallelExecution := false,
    addTestReportOption(AcceptanceTest, "acceptance-test-reports")
  )

  .settings(scalaVersion := "2.12.11")

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

val jacksonVersion = "2.10.4"

lazy val compile = Seq(
  ws,
  ehcache,
  "uk.gov.hmrc" %% "bootstrap-play-26" % "1.7.0",
  "uk.gov.hmrc" %% "url-builder" % "3.3.0-play-26",
  "uk.gov.hmrc" %% "http-metrics" % "1.10.0",
  "uk.gov.hmrc" %% "raml-tools" % "1.18.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.54.0-play-26",
  "uk.gov.hmrc" %% "play-ui" % "8.9.0-play-26",
  "org.raml" % "raml-parser-2" % "1.0.13",
  "uk.gov.hmrc" %% "play-partials" % "6.11.0-play-26",
  "io.dropwizard.metrics" % "metrics-graphite" % "3.2.0",
  "org.commonjava.googlecode.markdown4j" % "markdown4j" % "2.2-cj-1.1",

  /*
    Jackson dependencies are transitive but the same versions are not pulled in for each one.
    Therefore we explicitly pull them here at the same version to prevent problems running tests.
   */
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.module" % "jackson-module-jsonSchema" % jacksonVersion
)

lazy val testScopes = "test"

lazy val test = Seq(
  "io.cucumber" %% "cucumber-scala" % "5.7.0" % testScopes,
  "io.cucumber" % "cucumber-junit" % "5.7.0" % testScopes,
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-26" % testScopes,
  "org.pegdown" % "pegdown" % "1.6.0" % testScopes,
  "com.typesafe.play" %% "play-test" % PlayVersion.current % testScopes,
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3" % testScopes,
  "org.mockito" % "mockito-core" % "1.10.19" % testScopes,
  "org.seleniumhq.selenium" % "selenium-java" % "3.141.59" % testScopes,
  "org.seleniumhq.selenium" % "selenium-firefox-driver" % "3.141.59",
  "org.seleniumhq.selenium" % "selenium-chrome-driver" % "3.141.59",
  "com.github.tomakehurst" % "wiremock" % "1.58" % testScopes,
  "org.jsoup" % "jsoup" % "1.12.1" % testScopes
)

lazy val allDeps = compile ++ test

// Coverage configuration
// TODO ebridge - Fix and set back to 85
coverageMinimum := 64
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;uk.gov.hmrc.apidocumentation.config.*;.*javascript;testOnlyDoNotUseInAppConf.*;app.*;config.*"
