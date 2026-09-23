import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test

  lazy val bootstrapVersion       = "10.8.0"
  lazy val commonDomainVersion    = "1.4.0"
  lazy val apiDomainVersion       = "1.8.0"

  lazy val compile = Seq(
    caffeine,
    "uk.gov.hmrc"                         %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"                         %% "play-partials-play-30"      % "10.2.0",
    "uk.gov.hmrc"                         %% "play-frontend-hmrc-play-30" % "13.13.0",
    "uk.gov.hmrc"                         %% "api-platform-common-domain" % commonDomainVersion,
    "uk.gov.hmrc"                         %% "api-platform-api-domain"    % apiDomainVersion,
    "org.typelevel"                       %% "cats-core"                  % "2.10.0",
    "org.commonjava.googlecode.markdown4j" % "markdown4j"                 % "2.2-cj-1.1",
    "io.swagger.parser.v3"                 % "swagger-parser"             % "2.1.44",
    "org.playframework"                   %% "play-json"                  % "3.1.0-M10"
  )

  lazy val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"              % bootstrapVersion,
    "org.jsoup"               % "jsoup"                               % "1.23.2",
    "uk.gov.hmrc"            %% "ui-test-runner"                      % "0.56.0",
    "uk.gov.hmrc"            %% "api-platform-common-domain-fixtures" % commonDomainVersion
  ).map(_ % Test)
}
