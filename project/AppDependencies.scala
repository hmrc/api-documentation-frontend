import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test

  lazy val playJsonVersion = "2.9.2"
  lazy val bootstrapVersion = "7.14.0"
  lazy val seleniumVersion = "4.2.0"

  lazy val compile = Seq(
    ws,
    caffeine,
    "uk.gov.hmrc"                           %% "bootstrap-frontend-play-28"   % bootstrapVersion,
    "uk.gov.hmrc"                           %% "http-metrics"                 % "2.7.0",
    "uk.gov.hmrc"                           %% "play-partials"                % "8.4.0-play-28",
    "uk.gov.hmrc"                           %% "play-frontend-hmrc"           % "7.3.0-play-28",
    "org.typelevel"                         %% "cats-core"                    % "2.6.1",
    "org.commonjava.googlecode.markdown4j"  %  "markdown4j"                   % "2.2-cj-1.1",
    "com.typesafe.play"                     %% "play-json"                    % playJsonVersion,
    "com.typesafe.play"                     %% "play-json-joda"               % playJsonVersion
  )

  lazy val test = Seq(
    "uk.gov.hmrc"                           %% "bootstrap-test-play-28"       % bootstrapVersion,
    "io.cucumber"                           %% "cucumber-scala"               % "5.7.0",
    "io.cucumber"                           %  "cucumber-junit"               % "5.7.0",
    "org.scalatestplus"                     %% "selenium-4-2"                 % "3.2.13.0",
    "org.seleniumhq.selenium"               %  "selenium-remote-driver"       % seleniumVersion,
    "org.seleniumhq.selenium"               %  "selenium-firefox-driver"      % seleniumVersion,
    "org.seleniumhq.selenium"               %  "selenium-chrome-driver"       % seleniumVersion,
    "org.seleniumhq.selenium"               %  "selenium-remote-driver"       % seleniumVersion,
    "com.github.tomakehurst"                %  "wiremock-jre8-standalone"     % "2.35.0",
    "org.mockito"                           %% "mockito-scala-scalatest"      % "1.16.46",
    "com.vladsch.flexmark"                  %  "flexmark-all"                 % "0.62.2",
    "org.jsoup"                             %  "jsoup"                        % "1.12.1",
    "uk.gov.hmrc"                           %% "webdriver-factory"            % "0.41.0"
  ).map(_ % Test)
}
