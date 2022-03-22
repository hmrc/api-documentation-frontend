import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test

  lazy val compile = Seq(
    ws,
    caffeine,
    "uk.gov.hmrc"                           %% "bootstrap-frontend-play-28"   % "5.16.0",
    "uk.gov.hmrc"                           %% "url-builder"                  % "3.5.0-play-28",
    "uk.gov.hmrc"                           %% "http-metrics"                 % "2.5.0-play-28",
    "uk.gov.hmrc"                           %% "play-ui"                      % "9.7.0-play-28",
    "uk.gov.hmrc"                           %% "govuk-template"               % "5.72.0-play-28",
    "uk.gov.hmrc"                           %% "play-partials"                % "8.2.0-play-28",
    "uk.gov.hmrc"                           %% "play-frontend-hmrc"           % "1.26.0-play-28",
    "org.typelevel"                         %% "cats-core"                    % "2.6.1",
    "org.commonjava.googlecode.markdown4j"  %  "markdown4j"                   % "2.2-cj-1.1",
    "com.typesafe.play"                     %% "play-json"                    % "2.9.2",
    "com.typesafe.play"                     %% "play-json-joda"               % "2.9.2",
    "uk.gov.hmrc.objectstore"     %%  "object-store-client-play-28" % "0.39.0"
  )

  lazy val test = Seq(
    "uk.gov.hmrc"                           %% "bootstrap-test-play-28"       % "5.16.0",
    "io.cucumber"                           %% "cucumber-scala"               % "5.7.0",
    "io.cucumber"                           %  "cucumber-junit"               % "5.7.0",
    "org.pegdown"                           %  "pegdown"                      % "1.6.0",
    "org.mockito"                           %% "mockito-scala-scalatest"      % "1.16.46",
    "org.seleniumhq.selenium"               %  "selenium-java"                % "3.141.59",
    "org.seleniumhq.selenium"               %  "selenium-firefox-driver"      % "3.141.59",
    "org.seleniumhq.selenium"               %  "selenium-chrome-driver"       % "3.141.59",
    "com.github.tomakehurst"                %  "wiremock-jre8-standalone"     % "2.31.0",
    "org.jsoup"                             %  "jsoup"                        % "1.12.1"
  ).map(_ % "test")
}
