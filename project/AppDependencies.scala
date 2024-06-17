import play.sbt.PlayImport._
import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = compile ++ test

  lazy val bootstrapVersion       = "8.5.0"
  lazy val seleniumVersion        = "4.2.0"
  lazy val jacksonDatabindVersion = "2.10.5.1"
  lazy val jacksonVersion         = "2.10.5"
  lazy val commonDomainVersion    = "0.13.0"
  lazy val apiDomainVersion       = "0.15.0"

  lazy val compile = Seq(
    ws,
    caffeine,
    "uk.gov.hmrc"                         %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"                         %% "play-partials-play-30"      % "9.1.0",
    "uk.gov.hmrc"                         %% "http-metrics"               % "2.8.0",
    "uk.gov.hmrc"                         %% "play-frontend-hmrc-play-30" % "9.0.0",
    "uk.gov.hmrc"                         %% "api-platform-api-domain"    % apiDomainVersion,
    "org.typelevel"                       %% "cats-core"                  % "2.10.0",
    "org.commonjava.googlecode.markdown4j" % "markdown4j"                 % "2.2-cj-1.1",
    "io.swagger.parser.v3"                 % "swagger-parser"             % "2.1.9"
      excludeAll (
        ExclusionRule("com.fasterxml.jackson.core", "jackson-databind"),
        ExclusionRule("com.fasterxml.jackson.core", "jackson-core"),
        ExclusionRule("com.fasterxml.jackson.core", "jackson-annotations"),
        ExclusionRule("com.fasterxml.jackson.dataformat", "jackson-dataformat-yaml"),
        ExclusionRule("com.fasterxml.jackson.datatype", "jackson-datatype-jsr310")
      ),
    "com.fasterxml.jackson.core"           % "jackson-core"               % jacksonVersion,
    "com.fasterxml.jackson.core"           % "jackson-databind"           % jacksonDatabindVersion,
    "com.fasterxml.jackson.core"           % "jackson-annotations"        % jacksonVersion,
    "com.fasterxml.jackson.dataformat"     % "jackson-dataformat-yaml"    % jacksonVersion,
    "com.fasterxml.jackson.datatype"       % "jackson-datatype-jsr310"    % jacksonVersion
  )

  lazy val test = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"          % bootstrapVersion,
    "io.cucumber"            %% "cucumber-scala"                  % "5.7.0",
    "org.mockito"            %% "mockito-scala-scalatest"         % "1.17.30",
    "org.jsoup"               % "jsoup"                           % "1.12.1",
    "uk.gov.hmrc"            %% "ui-test-runner"                  % "0.31.0",
    "uk.gov.hmrc"            %% "api-platform-test-common-domain" % commonDomainVersion
  ).map(_ % Test)
}
