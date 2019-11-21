import _root_.play.core.PlayVersion
import _root_.play.sbt.PlayImport._
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.uglify.Import.{uglifyCompressOptions, _}
import com.typesafe.sbt.web.Import._
import net.ground5hark.sbt.concat.Import._
import play.sbt.routes.RoutesKeys.routesGenerator
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
  .enablePlugins(Seq(_root_.play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory) ++ plugins: _*)
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
  .settings(playSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    targetJvm := "jvm-1.8",
    libraryDependencies ++= appDependencies,
    parallelExecution in Test := false,
    fork in Test := false,
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    majorVersion := 0
  )
  .settings(playPublishingSettings: _*)
  .settings(unmanagedResourceDirectories in Compile += baseDirectory.value / "resources")
  .settings(inConfig(TemplateTest)(Defaults.testSettings): _*)
  .settings(testOptions in Test := Seq(Tests.Filter(unitFilter), Tests.Argument(TestFrameworks.ScalaTest, "-eT")))
  .configs(IntegrationTest)
  .settings(inConfig(TemplateItTest)(Defaults.itSettings): _*)
  .settings(
    Keys.fork in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := Seq((baseDirectory in IntegrationTest).value / "it"),
    unmanagedResourceDirectories in IntegrationTest := Seq((baseDirectory in IntegrationTest).value / "it/resources"),
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest := (definedTests in IntegrationTest).value.map {
      test => Group(test.name, Seq(test), SubProcess(ForkOptions(runJVMOptions = Seq("-Dtest.name=" + test.name))))
    },
    parallelExecution in IntegrationTest := false)
  .settings(
    resolvers += Resolver.bintrayRepo("hmrc", "releases"),
    resolvers += Resolver.jcenterRepo)
  .configs(EndToEndTest)
  .settings(inConfig(EndToEndTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in EndToEndTest := Seq(Tests.Filter(endToEndFilter)),
    unmanagedSourceDirectories in EndToEndTest := Seq((baseDirectory in EndToEndTest).value / "test"),
    unmanagedResourceDirectories in EndToEndTest := Seq((baseDirectory in EndToEndTest).value / "test"),
    Keys.fork in EndToEndTest := false,
    parallelExecution in EndToEndTest := false,
    addTestReportOption(EndToEndTest, "e2e-test-reports")
  )
  .configs(AcceptanceTest)
  .settings(inConfig(AcceptanceTest)(Defaults.testSettings): _*)
  .settings(
    testOptions in AcceptanceTest := Seq(Tests.Filter(acceptanceTestFilter), Tests.Argument(TestFrameworks.ScalaTest, "-eT")),
    unmanagedSourceDirectories in AcceptanceTest := Seq((baseDirectory in AcceptanceTest).value / "test"),
    unmanagedResourceDirectories in AcceptanceTest := Seq((baseDirectory in AcceptanceTest).value / "test", (baseDirectory in AcceptanceTest).value / "target/web/public/test"),
    Keys.fork in AcceptanceTest := false,
    parallelExecution in AcceptanceTest := false,
    addTestReportOption(AcceptanceTest, "acceptance-test-reports")
  )

  .settings(scalaVersion := "2.11.11")
lazy val allPhases = "tt->test;test->test;test->compile;compile->compile"
lazy val allItPhases = "tit->it;it->it;it->compile;compile->compile"
lazy val AcceptanceTest = config("acceptance") extend Test
lazy val EndToEndTest = config("endtoend") extend Test
lazy val TemplateTest = config("tt") extend Test
lazy val TemplateItTest = config("tit") extend IntegrationTest
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
  cache,
  "uk.gov.hmrc" %% "bootstrap-play-25" % "4.13.0",
  "uk.gov.hmrc" %% "url-builder" % "3.3.0-play-25",
  "uk.gov.hmrc" %% "http-metrics" % "1.5.0",
//  "uk.gov.hmrc" %% "raml-tools" % "1.11.0",
  "uk.gov.hmrc" %% "govuk-template" % "5.37.0-play-25",
  "uk.gov.hmrc" %% "play-ui" % "7.40.0-play-25",
  "org.raml" % "raml-parser-2" % "1.0.13",
  "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-25",
  "io.dropwizard.metrics" % "metrics-graphite" % "3.2.0",
  "jp.t2v" %% "play2-auth" % "0.14.2",
  "org.commonjava.googlecode.markdown4j" % "markdown4j" % "2.2-cj-1.1"
)

lazy val test = Seq(
  "info.cukes" %% "cucumber-scala" % "1.2.5" % "test,it",
  "info.cukes" % "cucumber-junit" % "1.2.5" % "test,it",
  "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % "test,it",
  "junit" % "junit" % "4.12" % "test,it",
  "org.pegdown" % "pegdown" % "1.6.0" % "test,it",
  "com.typesafe.play" %% "play-test" % PlayVersion.current % "test,it",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test,it",
  "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % "test,it",
  "org.mockito" % "mockito-all" % "1.10.19" % "test,it",
  "org.seleniumhq.selenium" % "selenium-java" % "2.53.1" % "test,it",
  "org.seleniumhq.selenium" % "selenium-htmlunit-driver" % "2.52.0",
  "com.github.tomakehurst" % "wiremock" % "1.58" % "test,it",
  "org.jsoup" % "jsoup" % "1.11.3" % "test,it",
  "jp.t2v" %% "play2-auth-test" % "0.14.2" % "test,it",
  "de.leanovate.play-mockws" %% "play-mockws" % "2.5.1" % "test"
).map(_.exclude("xalan", "xalan")
  .exclude("org.apache.httpcomponents", "httpcore")
)
lazy val allDeps = compile ++ test

def acceptanceTestFilter(name: String): Boolean = name startsWith "acceptance"

def endToEndFilter(name: String): Boolean = name startsWith "endtoend"

def unitFilter(name: String): Boolean = name startsWith "unit"

// Coverage configuration
coverageMinimum := 80
coverageFailOnMinimum := true
coverageExcludedPackages := "<empty>;com.kenshoo.play.metrics.*;.*definition.*;prod.*;uk.gov.hmrc.apidocumentation.config.*;testOnlyDoNotUseInAppConf.*;app.*;config.*"
