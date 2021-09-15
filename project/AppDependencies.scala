import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] =
    compile ++ test

  lazy val testScopes = "test"

  lazy val compile = Seq(
    ws,
    ehcache,
    "uk.gov.hmrc"                           %% "bootstrap-play-26"        % "4.0.0",
    "uk.gov.hmrc"                           %% "url-builder"              % "3.4.0-play-26",
    "uk.gov.hmrc"                           %% "http-metrics"             % "1.11.0",
    "uk.gov.hmrc"                           %% "govuk-template"           % "5.61.0-play-26",
    "uk.gov.hmrc"                           %% "play-partials"            % "6.11.0-play-26",
    "uk.gov.hmrc"                           %% "play-frontend-hmrc"       % "1.9.0-play-26",
    "io.dropwizard.metrics"                 %  "metrics-graphite"         % "3.2.0",
    "org.typelevel"                         %% "cats-core"                % "2.0.0",
    "org.commonjava.googlecode.markdown4j"  %  "markdown4j"               % "2.2-cj-1.1",
    "com.typesafe.play"                     %% "play-json"                % "2.8.1"
  )

  lazy val test = Seq(
    "io.cucumber"                           %% "cucumber-scala"           % "5.7.0",
    "io.cucumber"                           %  "cucumber-junit"           % "5.7.0",
    "org.pegdown"                           %  "pegdown"                  % "1.6.0",
    "com.typesafe.play"                     %% "play-test"                % PlayVersion.current,
    "org.scalatestplus.play"                %% "scalatestplus-play"       % "3.1.3",
    "org.mockito"                           %% "mockito-scala-scalatest"  % "1.7.1",
    "org.seleniumhq.selenium"               %  "selenium-java"            % "3.141.59",
    "org.seleniumhq.selenium"               %  "selenium-firefox-driver"  % "3.141.59",
    "org.seleniumhq.selenium"               %  "selenium-chrome-driver"   % "3.141.59",
    "com.github.tomakehurst"                %  "wiremock-jre8-standalone" % "2.27.1",
    "org.jsoup"                             %  "jsoup"                    % "1.12.1"
  ).map(_.withConfigurations(Some(testScopes)))
}
