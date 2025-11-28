import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test

  lazy val bootstrapVersion       = "10.4.0"
  lazy val commonDomainVersion    = "0.19.0"
  lazy val apiDomainVersion       = "0.20.0"

  lazy val compile = Seq(
    caffeine,
    "uk.gov.hmrc"                         %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"                         %% "play-partials-play-30"      % "10.2.0",
    "uk.gov.hmrc"                         %% "http-metrics"               % "2.9.0",
    "uk.gov.hmrc"                         %% "play-frontend-hmrc-play-30" % "12.20.0",
    "uk.gov.hmrc"                         %% "api-platform-api-domain"    % apiDomainVersion,
    "org.typelevel"                       %% "cats-core"                  % "2.10.0",
    "org.commonjava.googlecode.markdown4j" % "markdown4j"                 % "2.2-cj-1.1",
    "io.swagger.parser.v3"                 % "swagger-parser"             % "2.1.14",
    "commons-io"                           % "commons-io"                 % "2.14.0", // to fix CVE-2024-47554 until swagger-parser can be upgraded above 2.1.14
  )

  lazy val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"              % bootstrapVersion,
    "org.mockito"            %% "mockito-scala-scalatest"             % "2.0.0",
    "org.jsoup"               % "jsoup"                               % "1.12.1",
    "uk.gov.hmrc"            %% "ui-test-runner"                      % "0.50.0",
    "uk.gov.hmrc"            %% "api-platform-common-domain-fixtures" % commonDomainVersion
  ).map(_ % Test)
}
